package com.test.seckill.service;

import com.test.seckill.config.RabbitMQConfig;
import com.test.seckill.entity.LocalMessage;
import com.test.seckill.entity.SeckillMessage;
import com.test.seckill.mapper.LocalMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 消息补偿服务
 * 定时扫描未确认的本地消息，进行补偿处理
 * 重试策略：先重新发送MQ消息，超过最大重试次数后回滚Redis库存并转入死信队列
 */
@Service
public class MessageCompensationService {

    private static final Logger logger = LoggerFactory.getLogger(MessageCompensationService.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private static final long RETRY_INTERVAL_SECONDS = 60;

    @Autowired
    private LocalMessageMapper localMessageMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定时扫描待确认消息（每30秒执行一次）
     */
    @Scheduled(fixedDelay = 30000)
    public void scanPendingMessages() {
        try {
            List<LocalMessage> messages = localMessageMapper.selectPendingMessages(
                    LocalMessage.Status.PENDING.getCode(),
                    new Date()
            );

            if (messages.isEmpty()) {
                return;
            }

            logger.info("扫描到{}条待确认消息", messages.size());

            for (LocalMessage message : messages) {
                processMessage(message);
            }
        } catch (Exception e) {
            logger.error("扫描待确认消息失败", e);
        }
    }

    /**
     * 处理单条消息
     * @param message 消息对象
     */
    private void processMessage(LocalMessage message) {
        try {
            if (message.getRetryCount() >= message.getMaxRetryCount()) {
                logger.warn("消息超过最大重试次数({})，执行补偿并转入死信队列：消息ID={}", 
                    message.getMaxRetryCount(), message.getId());
                compensateAndSendToDeadLetter(message);
                return;
            }

            boolean sendSuccess = retrySendMessage(message);

            if (sendSuccess) {
                int newRetryCount = message.getRetryCount() + 1;
                long backoffInterval = RETRY_INTERVAL_SECONDS * (long) Math.pow(2, newRetryCount - 1);
                Date nextRetryTime = new Date(System.currentTimeMillis() + backoffInterval * 1000);

                localMessageMapper.updateRetryInfo(message.getId(), newRetryCount, nextRetryTime);

                logger.info("消息重新发送成功：消息ID={}, 当前重试次数={}, 下次重试时间={}", 
                    message.getId(), newRetryCount, nextRetryTime);
            } else {
                Date nextRetryTime = new Date(System.currentTimeMillis() + RETRY_INTERVAL_SECONDS * 1000);
                localMessageMapper.updateRetryInfo(message.getId(), message.getRetryCount(), nextRetryTime);

                logger.warn("消息重新发送失败，将在{}后重试：消息ID={}", nextRetryTime, message.getId());
            }

        } catch (Exception e) {
            logger.error("处理消息失败：消息ID={}", message.getId(), e);
        }
    }

    /**
     * 重新发送MQ消息
     * @param message 本地消息对象
     * @return true表示发送成功，false表示发送失败
     */
    private boolean retrySendMessage(LocalMessage message) {
        try {
            SeckillMessage seckillMessage = objectMapper.readValue(
                message.getMessageContent(), SeckillMessage.class);

            seckillMessage.setLocalMessageId(message.getId());
            seckillMessage.setMessageId(UUID.randomUUID().toString());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                seckillMessage
            );

            logger.info("重新发送MQ消息：消息ID={}, 本地消息ID={}, 用户ID={}, 商品ID={}",
                seckillMessage.getMessageId(), message.getId(), 
                seckillMessage.getUserId(), seckillMessage.getProductId());

            return true;

        } catch (JsonProcessingException e) {
            logger.error("解析消息内容失败：消息ID={}", message.getId(), e);
            return false;
        } catch (Exception e) {
            logger.error("发送MQ消息失败：消息ID={}", message.getId(), e);
            return false;
        }
    }

    /**
     * 执行补偿操作并转入死信队列
     * @param message 消息对象
     */
    private void compensateAndSendToDeadLetter(LocalMessage message) {
        try {
            rollbackRedisStock(message);
            sendToDeadLetterQueue(message);
            localMessageMapper.updateStatus(message.getId(), LocalMessage.Status.COMPENSATED.getCode());

            logger.error("消息补偿完成并转入死信队列：消息ID={}, 业务ID={}", 
                message.getId(), message.getBusinessId());

            sendAlert(message);

        } catch (Exception e) {
            logger.error("补偿失败：消息ID={}", message.getId(), e);
        }
    }

    /**
     * 回滚Redis库存
     * @param message 消息对象
     */
    private void rollbackRedisStock(LocalMessage message) {
        try {
            String businessId = message.getBusinessId();
            String[] parts = businessId.split(":");
            if (parts.length >= 2) {
                String productId = parts[1];
                String stockKey = STOCK_KEY_PREFIX + productId;
                redisTemplate.opsForValue().increment(stockKey);
                logger.info("补偿成功：Redis库存回滚，商品ID={}", productId);
            }
        } catch (Exception e) {
            logger.error("回滚Redis库存失败：消息ID={}", message.getId(), e);
        }
    }

    /**
     * 发送消息到死信队列
     * @param message 消息对象
     */
    private void sendToDeadLetterQueue(LocalMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEAD_LETTER_EXCHANGE_NAME,
                RabbitMQConfig.DEAD_LETTER_ROUTING_KEY,
                message.getMessageContent()
            );
            logger.info("消息已发送到死信队列：消息ID={}", message.getId());
        } catch (Exception e) {
            logger.error("发送到死信队列失败：消息ID={}", message.getId(), e);
        }
    }

    /**
     * 发送告警
     * @param message 消息对象
     */
    private void sendAlert(LocalMessage message) {
        logger.error("【告警】秒杀订单补偿失败，需要人工介入！消息ID={}, 业务ID={}, 重试次数={}",
            message.getId(), message.getBusinessId(), message.getRetryCount());
    }

    /**
     * 获取待确认消息数量
     * @return 待确认消息数量
     */
    public int getPendingMessageCount() {
        List<LocalMessage> messages = localMessageMapper.selectPendingMessages(
                LocalMessage.Status.PENDING.getCode(),
                new Date()
        );
        return messages.size();
    }
}