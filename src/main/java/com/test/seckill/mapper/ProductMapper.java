package com.test.seckill.mapper;

import com.test.seckill.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品Mapper接口
 */
@Mapper
public interface ProductMapper {
    /**
     * 根据ID查询商品
     * @param id 商品ID
     * @return 商品对象
     */
    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectById(Long id);
    
    /**
     * 查询所有商品
     * @return 商品列表
     */
    @Select("SELECT * FROM product")
    List<Product> selectAll();
    
    /**
     * 插入商品
     * @param product 商品对象
     * @return 影响行数
     */
    @Insert("INSERT INTO product (name, description, price, stock, create_time, update_time) VALUES (#{name}, #{description}, #{price}, #{stock}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);
    
    /**
     * 更新商品
     * @param product 商品对象
     * @return 影响行数
     */
    @Update("UPDATE product SET name = #{name}, description = #{description}, price = #{price}, stock = #{stock}, update_time = #{updateTime} WHERE id = #{id}")
    int update(Product product);
    
    /**
     * 删除商品
     * @param id 商品ID
     * @return 影响行数
     */
    @Delete("DELETE FROM product WHERE id = #{id}")
    int delete(Long id);
}