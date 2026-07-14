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
        "local ttl = math.ceil(capacity / rate) * 2 " +
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
}