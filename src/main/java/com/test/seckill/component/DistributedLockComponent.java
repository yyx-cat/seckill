package com.test.seckill.component;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁组件
 * 使用Redisson实现Redis分布式锁
 * 防止同一用户在同一秒杀中生成多个订单
 */
@Component
public class DistributedLockComponent {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockComponent.class);

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "seckill:lock:";

    /**
     * 默认等待时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 0;

    /**
     * 默认锁定时间（秒）
     */
    private static final long DEFAULT_LOCK_TIMEOUT = 5;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取秒杀锁
     * @param userId 用户ID
     * @param productId 商品ID
     * @return RLock实例
     */
    public RLock getSeckillLock(Long userId, Long productId) {
        String lockKey = LOCK_PREFIX + userId + ":" + productId;
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取秒杀锁
     * @param userId 用户ID
     * @param productId 商品ID
     * @return true表示获取成功，false表示获取失败
     */
    public boolean trySeckillLock(Long userId, Long productId) {
        RLock lock = getSeckillLock(userId, productId);
        try {
            return lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LOCK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放秒杀锁
     * @param userId 用户ID
     * @param productId 商品ID
     */
    public void releaseSeckillLock(Long userId, Long productId) {
        RLock lock = getSeckillLock(userId, productId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 执行带锁的任务
     * @param userId 用户ID
     * @param productId 商品ID
     * @param task 任务
     * @return true表示执行成功，false表示未获取到锁
     */
    public boolean executeWithLock(Long userId, Long productId, Runnable task) {
        RLock lock = getSeckillLock(userId, productId);
        try {
            if (lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                task.run();
                return true;
            }
            logger.warn("用户{}购买商品{}时未获取到分布式锁", userId, productId);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取分布式锁时被中断：用户{}，商品{}", userId, productId);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}