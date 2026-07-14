package com.test.seckill.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级缓存管理器
 * 管理Caffeine和Redis两级缓存
 * 根据缓存名称创建对应的多级缓存实例
 */
public class MultiLevelCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheManager.class);

    /**
     * 本地缓存管理器（Caffeine）
     */
    private final CacheManager localCacheManager;

    /**
     * 分布式缓存管理器（Redis）
     */
    private final CacheManager redisCacheManager;

    /**
     * 缓存实例缓存
     */
    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param localCacheManager 本地缓存管理器
     * @param redisCacheManager Redis缓存管理器
     */
    public MultiLevelCacheManager(CacheManager localCacheManager, CacheManager redisCacheManager) {
        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
        logger.info("多级缓存管理器初始化完成");
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public java.util.Collection<String> getCacheNames() {
        java.util.Set<String> cacheNames = new java.util.HashSet<>();
        cacheNames.addAll(localCacheManager.getCacheNames());
        cacheNames.addAll(redisCacheManager.getCacheNames());
        return cacheNames;
    }

    /**
     * 创建多级缓存实例
     * @param name 缓存名称
     * @return 多级缓存实例
     */
    private Cache createCache(String name) {
        Cache localCache = localCacheManager.getCache(name);
        Cache redisCache = redisCacheManager.getCache(name);
        
        if (localCache == null) {
            logger.warn("本地缓存未找到：{}", name);
        }
        if (redisCache == null) {
            logger.warn("Redis缓存未找到：{}", name);
        }
        
        MultiLevelCache cache = new MultiLevelCache(name, localCache, redisCache);
        logger.info("创建多级缓存：{}", name);
        return cache;
    }
}