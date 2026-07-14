package com.test.seckill.service;

import com.test.seckill.config.GlobalExceptionHandler.BusinessException;
import com.test.seckill.config.RabbitMQConfig;
import com.test.seckill.component.DistributedLockComponent;
import com.test.seckill.entity.LocalMessage;
import com.test.seckill.entity.SeckillMessage;
import com.test.seckill.entity.SeckillProduct;
import com.test.seckill.mapper.LocalMessageMapper;
import com.test.seckill.mapper.SeckillProductMapper;
import com.test.seckill.component.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 秒杀服务
 * 提供秒杀核心业务逻辑
 * 包括Redis扣库存、分布式锁、消息队列等
 */
@Service
public class SeckillService {

    private static final Logger logger = LoggerFactory.getLogger(SeckillService.class);

    /**
     * 库存key前缀
     */
    private static final String STOCK_KEY = "seckill:stock:";

    /**
     * 秒杀商品信息key前缀
     */
    private static final String PRODUCT_KEY = "seckill:product:";

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private LocalMessageMapper localMessageMapper;

    @Autowired
    private DistributedLockComponent distributedLockComponent;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisScript<Long> stockDeductScript;

    /**
     * 执行秒杀
     * 1. 获取分布式锁
     * 2. 检查库存
     * 3. Redis扣库存
     * 4. 创建本地消息
     * 5. 发送MQ消息
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 秒杀结果
     */
    public boolean seckill(Long userId, Long productId) {
        // 使用分布式锁防止重复下单
        boolean locked = distributedLockComponent.executeWithLock(userId, productId, () -> {
            doSeckill(userId, productId);
        });

        return locked;
    }

    /**
     * 执行秒杀核心逻辑
     * @param userId 用户ID
     * @param productId 商品ID
     */
    private void doSeckill(Long userId, Long productId) {
        // 1. 检查秒杀商品是否存在
        SeckillProduct seckillProduct = getSeckillProduct(productId);
        if (seckillProduct == null) {
            throw new BusinessException("秒杀商品不存在");
        }

        // 2. 检查秒杀是否处于活动状态
        if (!isSeckillActive(seckillProduct)) {
            throw new BusinessException("秒杀尚未开始或已结束");
        }

        // 3. Redis扣库存（原子操作）
        boolean success = deductStock(productId);
        if (!success) {
            throw new BusinessException("库存不足");
        }

        // 4. 创建本地消息（用于分布式事务最终一致性）
        LocalMessage localMessage = createLocalMessage(userId, productId, seckillProduct.getId());

        // 5. 发送MQ消息（异步创建订单）
        sendSeckillMessage(userId, productId, seckillProduct.getId(), localMessage.getId());

        logger.info("秒杀成功：userId={}, productId={}", userId, productId);
    }

    /**
     * 获取秒杀商品信息
     * @param productId 商品ID
     * @return 秒杀商品对象
     */
    public SeckillProduct getSeckillProduct(Long productId) {
        // 先从Redis获取
        String productKey = PRODUCT_KEY + productId;
        Object cachedProduct = redisTemplate.opsForValue().get(productKey);
        if (cachedProduct instanceof SeckillProduct) {
            return (SeckillProduct) cachedProduct;
        }

        // 从数据库获取
        SeckillProduct seckillProduct = seckillProductMapper.selectByProductId(productId);
        if (seckillProduct != null) {
            redisTemplate.opsForValue().set(productKey, seckillProduct);
        }
        return seckillProduct;
    }

    /**
     * 检查秒杀是否处于活动状态
     * 判断当前时间是否在开始时间之后且结束时间之前
     * @param seckillProduct 秒杀商品
     * @return true表示秒杀进行中，false表示尚未开始或已结束
     */
    private boolean isSeckillActive(SeckillProduct seckillProduct) {
        Date now = new Date();
        return now.after(seckillProduct.getStartTime()) && now.before(seckillProduct.getEndTime());
    }

    /**
     * Redis扣库存（原子操作，使用Lua脚本保证原子性）
     * @param productId 商品ID
     * @return true表示扣减成功，false表示库存不足
     */
    private boolean deductStock(Long productId) {
        String stockKey = STOCK_KEY + productId;
        List<String> keys = Arrays.asList(stockKey);
        
        Long result = redisTemplate.execute(
            stockDeductScript,
            keys,
            "1"
        );
        
        return result != null && result == 1;
    }

    /**
     * 创建本地消息
     * @param userId 用户ID
     * @param productId 商品ID
     * @param seckillProductId 秒杀商品ID
     * @return 本地消息对象
     */
    private LocalMessage createLocalMessage(Long userId, Long productId, Long seckillProductId) {
        LocalMessage message = new LocalMessage();
        message.setId(snowflakeIdGenerator.nextId());
        message.setBusinessId(userId + ":" + productId);
        message.setBusinessType(LocalMessage.BusinessType.SECKILL_ORDER.getCode());
        message.setStatus(LocalMessage.Status.PENDING.getCode());
        message.setRetryCount(0);
        message.setMaxRetryCount(MAX_RETRY_COUNT);
        message.setNextRetryTime(new Date());
        message.setCreateTime(new Date());
        message.setUpdateTime(new Date());

        // 构造消息内容
        SeckillMessage seckillMessage = new SeckillMessage();
        seckillMessage.setMessageId(UUID.randomUUID().toString());
        seckillMessage.setUserId(userId);
        seckillMessage.setProductId(productId);
        seckillMessage.setSeckillProductId(seckillProductId);
        seckillMessage.setTimestamp(System.currentTimeMillis());

        try {
            message.setMessageContent(objectMapper.writeValueAsString(seckillMessage));
        } catch (JsonProcessingException e) {
            logger.error("序列化消息失败", e);
            throw new BusinessException("序列化消息失败");
        }

        localMessageMapper.insert(message);
        logger.info("创建本地消息：messageId={}, businessId={}", message.getId(), message.getBusinessId());

        return message;
    }

    /**
     * 发送秒杀消息到MQ
     * @param userId 用户ID
     * @param productId 商品ID
     * @param seckillProductId 秒杀商品ID
     * @param localMessageId 本地消息ID
     */
    private void sendSeckillMessage(Long userId, Long productId, Long seckillProductId, Long localMessageId) {
        SeckillMessage message = new SeckillMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setProductId(productId);
        message.setSeckillProductId(seckillProductId);
        message.setLocalMessageId(localMessageId);
        message.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            message
        );

        logger.info("发送MQ消息：messageId={}, userId={}, productId={}", 
            message.getMessageId(), userId, productId);
    }

    /**
     * 确认本地消息
     * @param localMessageId 本地消息ID
     */
    public void confirmLocalMessage(Long localMessageId) {
        localMessageMapper.updateStatus(localMessageId, LocalMessage.Status.CONFIRMED.getCode());
        logger.info("确认本地消息：messageId={}", localMessageId);
    }
}