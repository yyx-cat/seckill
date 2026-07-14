package com.test.seckill.mapper;

import com.test.seckill.entity.OrderInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper {
    /**
     * 根据ID查询订单
     * @param id 订单ID
     * @return 订单对象
     */
    @Select("SELECT * FROM order_info WHERE id = #{id}")
    OrderInfo selectById(Long id);
    
    /**
     * 根据用户ID和商品ID查询订单
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 订单对象
     */
    @Select("SELECT * FROM order_info WHERE user_id = #{userId} AND product_id = #{productId}")
    OrderInfo selectByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @Select("SELECT * FROM order_info WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<OrderInfo> selectByUserId(Long userId);
    
    /**
     * 插入订单（手动指定ID）
     * @param orderInfo 订单对象
     * @return 影响行数
     */
    @Insert("INSERT INTO order_info (id, user_id, product_id, seckill_product_id, status, create_time) VALUES (#{id}, #{userId}, #{productId}, #{seckillProductId}, #{status}, #{createTime})")
    int insert(OrderInfo orderInfo);
    
    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态
     * @return 影响行数
     */
    @Update("UPDATE order_info SET status = #{status} WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
}