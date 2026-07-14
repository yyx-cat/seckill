package com.test.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 */
@Data
public class OrderInfo {
    /**
     * 订单ID（雪花算法生成）
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 秒杀商品ID
     */
    private Long seckillProductId;
    
    /**
     * 订单状态：0-待付款，1-已付款，2-已发货，3-已完成，4-已取消
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
}