package com.test.seckill.consumer;

import com.test.seckill.config.GlobalExceptionHandler.BusinessException;
import com.test.seckill.config.RabbitMQConfig;
import com.test.seckill.entity.SeckillMessage;
import com.test.seckill.service.OrderService;
import com.test.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀订单消费者
 * 消费MQ消息，异步创建订单
 */
@Component
public class SeckillOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private SeckillService seckillService;

    /**
     * 监听死信队列（重试失败的消息，需要人工介入）
     * @param messageContent 消息内容（JSON格式）
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE_NAME)
    public void handleDeadLetterMessage(String messageContent) {
        logger.error("【死信队列】收到需要人工介入的消息：{}", messageContent);
    }

    /**
     * 监听秒杀订单队列
     * @param message 消息内容
     * @param amqpMessage RabbitMQ原始消息
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void handleMessage(SeckillMessage message, Message amqpMessage) {
        logger.info("收到秒杀订单消息：messageId={}, userId={}, productId={}", 
            message.getMessageId(), message.getUserId(), message.getProductId());

        try {
            // 检查用户是否已购买（防止重复创建订单）
            boolean hasOrder = orderService.checkUserHasOrder(message.getUserId(), message.getProductId());
            if (hasOrder) {
                logger.warn("用户{}已购买商品{}，跳过订单创建", message.getUserId(), message.getProductId());
                // 确认本地消息（虽然不创建订单，但消息已处理完成）
                if (message.getLocalMessageId() != null) {
                    seckillService.confirmLocalMessage(message.getLocalMessageId());
                }
                return;
            }

            // 创建订单（使用雪花算法生成订单ID）
            orderService.createOrder(
                message.getUserId(),
                message.getProductId(),
                message.getSeckillProductId()
            );

            // 确认本地消息
            if (message.getLocalMessageId() != null) {
                seckillService.confirmLocalMessage(message.getLocalMessageId());
            }

            logger.info("订单创建成功：userId={}, productId={}", 
                message.getUserId(), message.getProductId());

        } catch (BusinessException e) {
            logger.error("业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("创建订单失败：userId={}, productId={}", 
                message.getUserId(), message.getProductId(), e);
            throw e;
        }
    }
}