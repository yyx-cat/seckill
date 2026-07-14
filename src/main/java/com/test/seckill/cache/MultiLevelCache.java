package com.test.seckill.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;

import java.util.concurrent.Callable;

/**
 * 多级缓存实现类
 * 实现Caffeine + Redis两级缓存
 * 查询顺序：Caffeine本地缓存 -> Redis缓存 -> 数据库
 * 写入顺序：同时写入本地缓存和Redis缓存
 */
public class MultiLevelCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);

    /**
     * 空值缓存标识
     */
    private static final String NULL_VALUE = "__NULL_VALUE__";

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 本地缓存（Caffeine）
     */
    private final Cache localCache;

    /**
     * 分布式缓存（Redis）
     */
    private final Cache redisCache;

    /**
     * 构造函数
     * @param name 缓存名称
     * @param localCache 本地缓存
     * @param redisCache Redis缓存
     */
    public MultiLevelCache(String name, Cache localCache, Cache redisCache) {
        this.name = name;
        this.localCache = localCache;
        this.redisCache = redisCache;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        String cacheKey = buildCacheKey(key);
        
        // 1. 先查本地缓存
        ValueWrapper localValue = localCache.get(cacheKey);
        if (localValue != null) {
            Object value = localValue.get();
            // 处理空值缓存
            if (NULL_VALUE.equals(value)) {
                logger.debug("本地缓存命中（空值）：{}", cacheKey);
                return null;
            }
            logger.debug("本地缓存命中：{}", cacheKey);
            return localValue;
        }

        // 2. 再查Redis缓存
        ValueWrapper redisValue = redisCache.get(cacheKey);
        if (redisValue != null) {
            Object value = redisValue.get();
            // 处理空值缓存
            if (NULL_VALUE.equals(value)) {
                logger.debug("Redis缓存命中（空值）：{}", cacheKey);
                // 将空值同步到本地缓存
                localCache.put(cacheKey, NULL_VALUE);
                return null;
            }
            logger.debug("Redis缓存命中：{}", cacheKey);
            // 将Redis缓存同步到本地缓存
            localCache.put(cacheKey, value);
            return redisValue;
        }

        logger.debug("缓存未命中：{}", cacheKey);
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        return (T) wrapper.get();
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String cacheKey = buildCacheKey(key);
        
        // 处理空值缓存（解决缓存穿透）
        if (value == null) {
            localCache.put(cacheKey, NULL_VALUE);
            redisCache.put(cacheKey, NULL_VALUE);
            logger.debug("缓存空值：{}", cacheKey);
        } else {
            localCache.put(cacheKey, value);
            redisCache.put(cacheKey, value);
            logger.debug("缓存写入：{}", cacheKey);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String cacheKey = buildCacheKey(key);
        
        ValueWrapper existing = get(cacheKey);
        if (existing == null) {
            put(key, value);
        }
        return existing;
    }

    @Override
    public void evict(Object key) {
        String cacheKey = buildCacheKey(key);
        localCache.evict(cacheKey);
        redisCache.evict(cacheKey);
        logger.debug("缓存删除：{}", cacheKey);
    }

    @Override
    public void clear() {
        localCache.clear();
        redisCache.clear();
        logger.debug("缓存清空：{}", name);
    }

    /**
     * 构建缓存键
     * 在键前添加缓存名称前缀，避免不同缓存间的key冲突
     * @param key 原始key
     * @return 完整的缓存键
     */
    private String buildCacheKey(Object key) {
        return name + ":" + key;
    }
}