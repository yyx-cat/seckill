package com.test.seckill.mapper;

import com.test.seckill.entity.SeckillProduct;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 秒杀商品Mapper接口
 */
@Mapper
public interface SeckillProductMapper {
    /**
     * 根据ID查询秒杀商品
     * @param id 秒杀商品ID
     * @return 秒杀商品对象
     */
    @Select("SELECT id, product_id, seckill_price, seckill_stock, start_time, end_time, status, create_time, update_time FROM seckill_product WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "seckillPrice", column = "seckill_price"),
        @Result(property = "seckillStock", column = "seckill_stock"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "status", column = "status"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    SeckillProduct selectById(Long id);
    
    /**
     * 根据商品ID查询秒杀商品
     * @param productId 商品ID
     * @return 秒杀商品对象
     */
    @Select("SELECT id, product_id, seckill_price, seckill_stock, start_time, end_time, status, create_time, update_time FROM seckill_product WHERE product_id = #{productId} AND status = 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "seckillPrice", column = "seckill_price"),
        @Result(property = "seckillStock", column = "seckill_stock"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "status", column = "status"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    SeckillProduct selectByProductId(Long productId);
    
    /**
     * 查询所有进行中的秒杀商品
     * @return 秒杀商品列表
     */
    @Select("SELECT id, product_id, seckill_price, seckill_stock, start_time, end_time, status, create_time, update_time FROM seckill_product WHERE status = 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "seckillPrice", column = "seckill_price"),
        @Result(property = "seckillStock", column = "seckill_stock"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "status", column = "status"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    List<SeckillProduct> selectActiveSeckillProducts();
    
    /**
     * 插入秒杀商品
     * @param seckillProduct 秒杀商品对象
     * @return 影响行数
     */
    @Insert("INSERT INTO seckill_product (product_id, seckill_price, seckill_stock, start_time, end_time, status, create_time, update_time) VALUES (#{productId}, #{seckillPrice}, #{seckillStock}, #{startTime}, #{endTime}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillProduct seckillProduct);
    
    /**
     * 更新秒杀商品
     * @param seckillProduct 秒杀商品对象
     * @return 影响行数
     */
    @Update("UPDATE seckill_product SET seckill_price = #{seckillPrice}, seckill_stock = #{seckillStock}, start_time = #{startTime}, end_time = #{endTime}, status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int update(SeckillProduct seckillProduct);
    
    /**
     * 删除秒杀商品
     * @param id 秒杀商品ID
     * @return 影响行数
     */
    @Delete("DELETE FROM seckill_product WHERE id = #{id}")
    int delete(Long id);
}