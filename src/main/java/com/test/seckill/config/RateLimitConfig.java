package com.test.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 分布式限流配置类
 * 使用Redis + Lua脚本实现令牌桶限流算法
 */
@Configuration
public class RateLimitConfig {

    /**
     * 令牌桶限流Lua脚本
     */
    private static final String TOKEN_BUCKET_SCRIPT =
        "local tokenKey = KEYS[1] " +
        "local timeKey = KEYS[2] " +
        "local capacity = tonumber(ARGV[1]) " +
        "local rate = tonumber(ARGV[2]) " +
        "local requested = tonumber(ARGV[3]) " +
        "local now = tonumber(ARGV[4]) " +

        "local currentTokens = tonumber(redis.call('get', tokenKey) or capacity) " +
        "local lastTime = tonumber(redis.call('get', timeKey) or now) " +

        "local delta = (now - lastTime) / 1000 " +
        "local filledTokens = math.min(capacity, currentTokens + delta * rate) " +

        "local allowed = 0 " +
        "local newTokens = filledTokens " +

        "if filledTokens >= requested then " +
        "    newTokens = filledTokens - requested " +
        "    allowed = 1 " +
        "end " +

        "redis.call('set', tokenKey, newTokens) " +
        "redis.call('set', timeKey, now) " +
        "local ttl = 3600 " +
        "redis.call('expire', tokenKey, ttl) " +
        "redis.call('expire', timeKey, ttl) " +

        "return allowed";

    /**
 * 令牌桶限流脚本Bean
 * @return RedisScript实例
 */
@Bean
public RedisScript<Long> tokenBucketScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(TOKEN_BUCKET_SCRIPT);
    script.setResultType(Long.class);
    return script;
}

/**
 * 库存扣减Lua脚本
 * KEYS[1]: 库存key
 * ARGV[1]: 扣减数量
 * 返回值: 1-扣减成功, 0-库存不足
 */
private static final String STOCK_DEDUCT_SCRIPT =
    "local stock = redis.call('get', KEYS[1]) " +
    "if stock and tonumber(stock) > 0 then " +
    "    redis.call('decr', KEYS[1]) " +
    "    return 1 " +
    "else " +
    "    return 0 " +
    "end";

/**
 * 库存扣减脚本Bean
 * @return RedisScript实例
 */
@Bean
public RedisScript<Long> stockDeductScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(STOCK_DEDUCT_SCRIPT);
    script.setResultType(Long.class);
    return script;
}

/**
 * 防刷计数器Lua脚本
 * KEYS[1]: 计数key
 * ARGV[1]: 过期时间（秒）
 * 返回值: 当前计数
 */
private static final String ANTI_BRUSH_SCRIPT =
    "local count = redis.call('incr', KEYS[1]) " +
    "redis.call('expire', KEYS[1], ARGV[1]) " +
    "return count";

/**
 * 防刷计数器脚本Bean
 * @return RedisScript实例
 */
@Bean
public RedisScript<Long> antiBrushScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(ANTI_BRUSH_SCRIPT);
    script.setResultType(Long.class);
    return script;
}
}