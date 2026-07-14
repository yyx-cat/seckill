package com.test.seckill.service;

import com.test.seckill.entity.Product;
import com.test.seckill.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 商品服务
 * 提供商品的CRUD操作
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductMapper productMapper;

    /**
     * 根据ID查询商品（带缓存）
     * @param id 商品ID
     * @return 商品对象
     */
    @Cacheable(value = "product", key = "#id")
    public Product getProductById(Long id) {
        logger.info("查询商品：id={}", id);
        return productMapper.selectById(id);
    }

    /**
     * 查询所有商品
     * @return 商品列表
     */
    public List<Product> getAllProducts() {
        logger.info("查询所有商品");
        return productMapper.selectAll();
    }

    /**
     * 创建商品
     * @param product 商品对象
     * @return 创建后的商品对象
     */
    public Product createProduct(Product product) {
        Date now = new Date();
        product.setCreateTime(now);
        product.setUpdateTime(now);
        productMapper.insert(product);
        logger.info("创建商品：id={}, name={}", product.getId(), product.getName());
        return product;
    }

    /**
     * 更新商品
     * @param product 商品对象
     * @return 更新后的商品对象
     */
    public Product updateProduct(Product product) {
        product.setUpdateTime(new Date());
        productMapper.update(product);
        logger.info("更新商品：id={}, name={}", product.getId(), product.getName());
        return product;
    }

    /**
     * 删除商品
     * @param id 商品ID
     */
    public void deleteProduct(Long id) {
        productMapper.delete(id);
        logger.info("删除商品：id={}", id);
    }
}