package com.test.seckill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 隐藏地址服务
 * 生成和验证秒杀隐藏地址
 * 防止恶意用户提前获取秒杀接口
 */
@Service
public class SeckillUrlService {

    private static final Logger logger = LoggerFactory.getLogger(SeckillUrlService.class);

    /**
     * 隐藏地址key前缀
     */
    private static final String SECKILL_URL_KEY = "seckill:url:";

    /**
     * 隐藏地址有效期（分钟）
     */
    private static final int URL_EXPIRE_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成秒杀隐藏地址
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 隐藏地址token
     */
    public String generateSeckillUrl(Long userId, Long productId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = SECKILL_URL_KEY + token;
        
        // 存储token对应的用户和商品信息
        String value = userId + ":" + productId;
        redisTemplate.opsForValue().set(key, value, URL_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        logger.info("生成秒杀隐藏地址：userId={}, productId={}, token={}", userId, productId, token);
        
        return token;
    }

    /**
     * 验证秒杀隐藏地址
     * @param token 隐藏地址token
     * @param userId 用户ID
     * @param productId 商品ID
     * @return true表示验证通过
     */
    public boolean validateSeckillUrl(String token, Long userId, Long productId) {
        String key = SECKILL_URL_KEY + token;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            logger.warn("秒杀隐藏地址无效或已过期：token={}", token);
            return false;
        }

        String expectedValue = userId + ":" + productId;
        if (!expectedValue.equals(value.toString())) {
            logger.warn("秒杀隐藏地址不匹配：token={}, expected={}, actual={}", 
                token, expectedValue, value);
            return false;
        }

        // 验证通过后删除token（防止重复使用）
        redisTemplate.delete(key);
        logger.info("秒杀隐藏地址验证通过：userId={}, productId={}", userId, productId);
        
        return true;
    }

    /**
     * 检查token是否存在
     * @param token 隐藏地址token
     * @return true表示存在
     */
    public boolean existsToken(String token) {
        String key = SECKILL_URL_KEY + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}