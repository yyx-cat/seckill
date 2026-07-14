package com.test.seckill.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 分布式限流组件
 * 使用Redis + Lua脚本实现令牌桶限流算法
 * 支持全局限流、用户级限流和IP级限流
 * 多实例部署时共享限流状态，实现真正的全局限流
 */
@Component
public class RateLimitComponent {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitComponent.class);

    /**
     * 全局限流key前缀
     */
    private static final String GLOBAL_LIMIT_KEY = "seckill:limit:global";

    /**
     * 用户限流key前缀
     */
    private static final String USER_LIMIT_KEY_PREFIX = "seckill:limit:user:";

    /**
     * IP限流key前缀
     */
    private static final String IP_LIMIT_KEY_PREFIX = "seckill:limit:ip:";

    /**
     * 全局限流：每秒1000个请求（桶容量2000，支持突发流量）
     */
    private static final long GLOBAL_CAPACITY = 2000;
    private static final double GLOBAL_RATE = 1000.0;

    /**
     * 用户限流：每秒5个请求（桶容量10，支持突发）
     */
    private static final long USER_CAPACITY = 10;
    private static final double USER_RATE = 5.0;

    /**
     * IP限流：每秒100个请求（桶容量200，支持突发）
     */
    private static final long IP_CAPACITY = 200;
    private static final double IP_RATE = 100.0;

    /**
     * 每次请求获取的令牌数
     */
    private static final long REQUESTED_TOKENS = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisScript<Long> tokenBucketScript;

    /**
     * 尝试获取全局限流许可
     * @return true表示获取成功，false表示被限流
     */
    public boolean tryAcquireGlobal() {
        return tryAcquire(GLOBAL_LIMIT_KEY, GLOBAL_CAPACITY, GLOBAL_RATE, REQUESTED_TOKENS);
    }

    /**
     * 尝试获取用户限流许可
     * @param userId 用户ID
     * @return true表示获取成功，false表示被限流
     */
    public boolean tryAcquireUser(Long userId) {
        String key = USER_LIMIT_KEY_PREFIX + userId;
        return tryAcquire(key, USER_CAPACITY, USER_RATE, REQUESTED_TOKENS);
    }

    /**
     * 尝试获取IP限流许可
     * @param ip IP地址
     * @return true表示获取成功，false表示被限流
     */
    public boolean tryAcquireIp(String ip) {
        String key = IP_LIMIT_KEY_PREFIX + ip;
        return tryAcquire(key, IP_CAPACITY, IP_RATE, REQUESTED_TOKENS);
    }

    /**
     * 同时检查全局限流和用户限流
     * @param userId 用户ID
     * @return true表示通过，false表示被限流
     */
    public boolean checkGlobalAndUser(Long userId) {
        return tryAcquireGlobal() && tryAcquireUser(userId);
    }

    /**
     * 同时检查全局、用户和IP限流
     * @param userId 用户ID
     * @param ip IP地址
     * @return true表示通过，false表示被限流
     */
    public boolean checkAll(Long userId, String ip) {
        return tryAcquireGlobal() && tryAcquireUser(userId) && tryAcquireIp(ip);
    }

    /**
     * 执行令牌桶限流（核心方法）
     * 使用Lua脚本保证原子性
     * @param keyPrefix 限流key前缀
     * @param capacity 桶容量
     * @param rate 每秒生成令牌数
     * @param requested 请求的令牌数
     * @return true表示获取成功，false表示被限流
     */
    private boolean tryAcquire(String keyPrefix, long capacity, double rate, long requested) {
        try {
            String tokenKey = keyPrefix + ":tokens";
            String timeKey = keyPrefix + ":time";
            List<String> keys = Arrays.asList(tokenKey, timeKey);

            long now = System.currentTimeMillis();

            Long result = redisTemplate.execute(
                tokenBucketScript,
                keys,
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(requested),
                String.valueOf(now)
            );

            boolean allowed = result != null && result == 1;

            if (!allowed) {
                logger.warn("限流触发：key={}, 容量={}, 速率={}/秒", keyPrefix, capacity, rate);
            }

            return allowed;

        } catch (Exception e) {
            logger.error("限流检查异常，默认放行：key={}", keyPrefix, e);
            return true;
        }
    }

    /**
     * 获取全局限流剩余令牌数（用于监控）
     * @return 剩余令牌数
     */
    public long getGlobalRemainingTokens() {
        return getRemainingTokens(GLOBAL_LIMIT_KEY);
    }

    /**
     * 获取用户限流剩余令牌数（用于监控）
     * @param userId 用户ID
     * @return 剩余令牌数
     */
    public long getUserRemainingTokens(Long userId) {
        return getRemainingTokens(USER_LIMIT_KEY_PREFIX + userId);
    }

    /**
     * 获取IP限流剩余令牌数（用于监控）
     * @param ip IP地址
     * @return 剩余令牌数
     */
    public long getIpRemainingTokens(String ip) {
        return getRemainingTokens(IP_LIMIT_KEY_PREFIX + ip);
    }

    /**
     * 获取剩余令牌数
     * @param keyPrefix 限流key前缀
     * @return 剩余令牌数
     */
    private long getRemainingTokens(String keyPrefix) {
        try {
            String tokenKey = keyPrefix + ":tokens";
            Object value = redisTemplate.opsForValue().get(tokenKey);
            if (value != null) {
                return Long.parseLong(value.toString());
            }
            return 0;
        } catch (Exception e) {
            logger.error("获取剩余令牌数失败：key={}", keyPrefix, e);
            return 0;
        }
    }
}