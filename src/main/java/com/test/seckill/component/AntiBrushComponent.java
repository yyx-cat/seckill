package com.test.seckill.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 防刷组件
 * 使用Redis实现IP和用户级别的访问频率限制
 * 防止恶意刷接口
 */
@Component
public class AntiBrushComponent {

    private static final Logger logger = LoggerFactory.getLogger(AntiBrushComponent.class);

    /**
     * IP访问次数key前缀
     */
    private static final String IP_ACCESS_COUNT_KEY = "seckill:antibrush:ip:";

    /**
     * 用户访问次数key前缀
     */
    private static final String USER_ACCESS_COUNT_KEY = "seckill:antibrush:user:";

    /**
     * IP黑名单key前缀
     */
    private static final String IP_BLACKLIST_KEY = "seckill:antibrush:blacklist:ip:";

    /**
     * 用户黑名单key前缀
     */
    private static final String USER_BLACKLIST_KEY = "seckill:antibrush:blacklist:user:";

    /**
     * 默认时间窗口（秒）
     */
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    /**
     * 默认最大访问次数
     */
    private static final int DEFAULT_MAX_COUNT = 100;

    /**
     * 默认黑名单时长（分钟）
     */
    private static final int DEFAULT_BLACKLIST_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查IP是否在黑名单中
     * @param ip IP地址
     * @return true表示在黑名单中
     */
    public boolean isIpBlacklisted(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IP_BLACKLIST_KEY + ip));
    }

    /**
     * 检查用户是否在黑名单中
     * @param userId 用户ID
     * @return true表示在黑名单中
     */
    public boolean isUserBlacklisted(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_BLACKLIST_KEY + userId));
    }

    /**
     * 将IP加入黑名单
     * @param ip IP地址
     */
    public void blacklistIp(String ip) {
        redisTemplate.opsForValue().set(IP_BLACKLIST_KEY + ip, "1", DEFAULT_BLACKLIST_MINUTES, TimeUnit.MINUTES);
        logger.warn("IP已加入黑名单：{}", ip);
    }

    /**
     * 将用户加入黑名单
     * @param userId 用户ID
     */
    public void blacklistUser(Long userId) {
        redisTemplate.opsForValue().set(USER_BLACKLIST_KEY + userId, "1", DEFAULT_BLACKLIST_MINUTES, TimeUnit.MINUTES);
        logger.warn("用户已加入黑名单：{}", userId);
    }

    /**
     * 检查IP访问频率
     * @param ip IP地址
     * @return true表示通过检查，false表示访问过于频繁
     */
    public boolean checkIpAccess(String ip) {
        return checkAccess(IP_ACCESS_COUNT_KEY + ip, DEFAULT_MAX_COUNT, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * 检查用户访问频率
     * @param userId 用户ID
     * @return true表示通过检查，false表示访问过于频繁
     */
    public boolean checkUserAccess(Long userId) {
        return checkAccess(USER_ACCESS_COUNT_KEY + userId, DEFAULT_MAX_COUNT, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * 检查访问频率
     * @param key Redis key
     * @param maxCount 最大访问次数
     * @param windowSeconds 时间窗口（秒）
     * @return true表示通过检查，false表示访问过于频繁
     */
    private boolean checkAccess(String key, int maxCount, int windowSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (count > maxCount) {
                logger.warn("访问过于频繁：key={}, 次数={}", key, count);
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("检查访问频率失败：key={}", key, e);
            return true;
        }
    }

    /**
     * 同时检查IP和用户访问频率
     * @param ip IP地址
     * @param userId 用户ID
     * @return true表示通过检查，false表示被拦截
     */
    public boolean checkAccess(String ip, Long userId) {
        if (isIpBlacklisted(ip)) {
            logger.warn("IP在黑名单中：{}", ip);
            return false;
        }

        if (userId != null && isUserBlacklisted(userId)) {
            logger.warn("用户在黑名单中：{}", userId);
            return false;
        }

        if (!checkIpAccess(ip)) {
            blacklistIp(ip);
            return false;
        }

        if (userId != null && !checkUserAccess(userId)) {
            blacklistUser(userId);
            return false;
        }

        return true;
    }
}