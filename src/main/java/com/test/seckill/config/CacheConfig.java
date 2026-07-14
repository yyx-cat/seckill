package com.test.seckill.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.test.seckill.cache.MultiLevelCache;
import com.test.seckill.cache.MultiLevelCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置类
 * 配置Caffeine本地缓存和Redis缓存
 * 支持多级缓存（Caffeine + Redis）
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 本地缓存管理器（Caffeine）
     * @return CaffeineCacheManager实例
     */
    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .expireAfterAccess(1, TimeUnit.HOURS));
        return cacheManager;
    }

    /**
     * Redis缓存管理器
     * @param redisConnectionFactory Redis连接工厂
     * @return RedisCacheManager实例
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        cacheConfigurations.put("seckillProduct", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(java.time.Duration.ofHours(1)));
        
        cacheConfigurations.put("product", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(java.time.Duration.ofHours(2)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 多级缓存管理器（主管理器）
     * 优先使用本地缓存，其次Redis缓存，最后数据库
     * @param localCacheManager 本地缓存管理器
     * @param redisCacheManager Redis缓存管理器
     * @return MultiLevelCacheManager实例
     */
    @Bean
    @Primary
    public CacheManager cacheManager(CacheManager localCacheManager, CacheManager redisCacheManager) {
        return new MultiLevelCacheManager(localCacheManager, redisCacheManager);
    }
}