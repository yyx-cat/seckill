package com.test.seckill.controller;

import com.test.seckill.entity.OrderInfo;
import com.test.seckill.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * 提供订单查询和管理接口
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    /**
     * 根据ID查询订单
     * @param id 订单ID
     * @return 订单对象
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderInfo> getOrderById(@PathVariable Long id) {
        logger.info("查询订单：id={}", id);
        OrderInfo order = orderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderInfo>> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("查询用户订单：userId={}", userId);
        List<OrderInfo> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态
     * @return 更新后的订单对象
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderInfo> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        logger.info("更新订单状态：id={}, status={}", id, status);
        OrderInfo order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }
}