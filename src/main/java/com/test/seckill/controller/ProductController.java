package com.test.seckill.controller;

import com.test.seckill.entity.Product;
import com.test.seckill.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 * 提供商品的CRUD接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * 查询所有商品
     * @return 商品列表
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("查询所有商品");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 根据ID查询商品
     * @param id 商品ID
     * @return 商品对象
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.info("查询商品：id={}", id);
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * 创建商品
     * @param product 商品对象
     * @return 创建后的商品对象
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        logger.info("创建商品：name={}", product.getName());
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.ok(createdProduct);
    }

    /**
     * 更新商品
     * @param id 商品ID
     * @param product 商品对象
     * @return 更新后的商品对象
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        logger.info("更新商品：id={}", id);
        product.setId(id);
        Product updatedProduct = productService.updateProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * 删除商品
     * @param id 商品ID
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("删除商品：id={}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}