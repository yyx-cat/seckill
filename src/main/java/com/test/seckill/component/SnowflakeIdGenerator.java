package com.test.seckill.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 雪花算法ID生成器
 * 生成全局唯一的分布式ID
 * 结构：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
 */
@Component
public class SnowflakeIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /**
     * 开始时间戳（2023-01-01 00:00:00）
     */
    private static final long START_TIMESTAMP = 1672531200000L;

    /**
     * 机器ID位数
     */
    private static final long WORKER_ID_BITS = 10L;

    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值
     */
    private static final long MAX_WORKER_ID = (1 << WORKER_ID_BITS) - 1;

    /**
     * 序列号最大值
     */
    private static final long MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1;

    /**
     * 机器ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 机器ID
     */
    private long workerId;

    /**
     * 当前序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * Redis key前缀
     */
    private static final String WORKER_ID_KEY = "seckill:snowflake:worker_id";

    /**
     * Redis key过期时间（秒）- 24小时
     */
    private static final long WORKER_ID_EXPIRE_SECONDS = 86400L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化工作ID
     * 通过Redis自增生成唯一的workerId
     */
    @PostConstruct
    public void init() {
        try {
            Long id = redisTemplate.opsForValue().increment(WORKER_ID_KEY);
            if (id == null || id <= 0) {
                id = 1L;
            }
            // 取模确保不超过最大值
            this.workerId = id % (MAX_WORKER_ID + 1);
            redisTemplate.expire(WORKER_ID_KEY, WORKER_ID_EXPIRE_SECONDS, TimeUnit.SECONDS);
            logger.info("雪花算法ID生成器初始化成功，workerId={}", workerId);
        } catch (Exception e) {
            logger.error("获取workerId失败，使用默认值0", e);
            this.workerId = 0;
        }
    }

    /**
     * 生成下一个ID
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long currentTimestamp = getCurrentTimestamp();

        // 如果当前时间戳小于上次生成ID的时间戳，说明时钟回拨
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                "时钟回拨，拒绝生成ID！lastTimestamp=%d, currentTimestamp=%d",
                lastTimestamp, currentTimestamp));
        }

        // 如果当前时间戳等于上次生成ID的时间戳，序列号自增
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 如果序列号达到最大值，等待下一个毫秒
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // 如果当前时间戳大于上次生成ID的时间戳，重置序列号
            sequence = 0L;
        }

        // 更新上次生成ID的时间戳
        lastTimestamp = currentTimestamp;

        // 生成ID
        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 获取当前时间戳（毫秒）
     * @return 当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一个毫秒
     * @param currentTimestamp 当前时间戳
     * @return 新的时间戳
     */
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }

    /**
     * 获取当前workerId
     * @return workerId
     */
    public long getWorkerId() {
        return workerId;
    }
}