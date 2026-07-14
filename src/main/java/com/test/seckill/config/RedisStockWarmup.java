package com.test.seckill.config;

import com.test.seckill.entity.SeckillProduct;
import com.test.seckill.mapper.SeckillProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 库存预热组件
 * 将数据库中的秒杀商品库存预热到Redis中
 * 避免秒杀开始时大量请求直接访问数据库
 */
@Component
public class RedisStockWarmup {

    private static final Logger logger = LoggerFactory.getLogger(RedisStockWarmup.class);

    /**
     * 库存key前缀
     */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /**
     * 秒杀商品信息key前缀
     */
    private static final String PRODUCT_KEY_PREFIX = "seckill:product:";

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 应用启动时执行库存预热
     */
    @PostConstruct
    public void warmupOnStartup() {
        logger.info("开始执行库存预热...");
        warmupStock();
        logger.info("库存预热完成");
    }

    /**
     * 执行库存预热
     */
    public void warmupStock() {
        try {
            // 查询所有进行中的秒杀商品
            List<SeckillProduct> products = seckillProductMapper.selectActiveSeckillProducts();

            if (products.isEmpty()) {
                logger.info("没有进行中的秒杀商品，跳过库存预热");
                return;
            }

            for (SeckillProduct product : products) {
                // 设置库存到Redis
                String stockKey = STOCK_KEY_PREFIX + product.getProductId();
                redisTemplate.opsForValue().set(stockKey, product.getSeckillStock());

                // 设置商品信息到Redis
                String productKey = PRODUCT_KEY_PREFIX + product.getProductId();
                redisTemplate.opsForValue().set(productKey, product);

                logger.info("库存预热完成：商品ID={}, 秒杀价格={}, 库存={}",
                    product.getProductId(), product.getSeckillPrice(), product.getSeckillStock());
            }
        } catch (Exception e) {
            logger.error("库存预热失败", e);
        }
    }

    /**
     * 定时刷新库存（每5分钟执行一次）
     * 用于同步数据库中的库存变化
     */
    @Scheduled(fixedDelay = 300000)
    public void refreshStock() {
        logger.info("定时刷新库存开始...");
        warmupStock();
        logger.info("定时刷新库存完成");
    }

    /**
     * 手动刷新指定商品的库存
     * @param productId 商品ID
     */
    public void refreshStock(Long productId) {
        try {
            SeckillProduct product = seckillProductMapper.selectByProductId(productId);
            if (product != null) {
                String stockKey = STOCK_KEY_PREFIX + productId;
                redisTemplate.opsForValue().set(stockKey, product.getSeckillStock());
                logger.info("手动刷新库存完成：商品ID={}, 库存={}", productId, product.getSeckillStock());
            }
        } catch (Exception e) {
            logger.error("手动刷新库存失败：商品ID={}", productId, e);
        }
    }
}