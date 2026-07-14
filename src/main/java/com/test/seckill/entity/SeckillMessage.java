package com.test.seckill.entity;

import lombok.Data;

/**
 * 秒杀消息实体类
 * 用于MQ消息传递
 */
@Data
public class SeckillMessage {
    /**
     * 消息ID
     */
    private String messageId;
    
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
     * 本地消息ID
     */
    private Long localMessageId;
    
    /**
     * 创建时间戳
     */
    private Long timestamp;
}