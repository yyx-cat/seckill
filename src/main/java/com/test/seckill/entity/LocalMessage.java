package com.test.seckill.entity;

import lombok.Data;

import java.util.Date;

/**
 * 本地消息实体类
 * 用于分布式事务最终一致性
 */
@Data
public class LocalMessage {
    /**
     * 消息ID（雪花算法生成）
     */
    private Long id;
    
    /**
     * 业务ID（格式：userId:productId）
     */
    private String businessId;
    
    /**
     * 业务类型
     */
    private Integer businessType;
    
    /**
     * 消息内容（JSON格式）
     */
    private String messageContent;
    
    /**
     * 状态：0-待确认，1-已确认，2-已补偿
     */
    private Integer status;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    
    /**
     * 下次重试时间
     */
    private Date nextRetryTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 业务类型枚举
     */
    public enum BusinessType {
        /**
         * 秒杀订单
         */
        SECKILL_ORDER(1);
        
        private final int code;
        
        BusinessType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
    }
    
    /**
     * 状态枚举
     */
    public enum Status {
        /**
         * 待确认
         */
        PENDING(0),
        /**
         * 已确认
         */
        CONFIRMED(1),
        /**
         * 已补偿
         */
        COMPENSATED(2);
        
        private final int code;
        
        Status(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
    }
}