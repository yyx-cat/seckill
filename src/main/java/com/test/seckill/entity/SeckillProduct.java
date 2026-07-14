package com.test.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀商品实体类
 */
@Data
public class SeckillProduct {
    /**
     * 秒杀商品ID
     */
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    
    /**
     * 秒杀库存
     */
    private Integer seckillStock;
    
    /**
     * 秒杀开始时间
     */
    private Date startTime;
    
    /**
     * 秒杀结束时间
     */
    private Date endTime;
    
    /**
     * 状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}