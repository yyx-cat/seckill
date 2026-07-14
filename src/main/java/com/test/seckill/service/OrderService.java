package com.test.seckill.service;

import com.test.seckill.component.SnowflakeIdGenerator;
import com.test.seckill.entity.OrderInfo;
import com.test.seckill.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 订单服务
 * 提供订单的查询和管理操作
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 根据ID查询订单
     * @param id 订单ID
     * @return 订单对象
     */
    public OrderInfo getOrderById(Long id) {
        logger.info("查询订单：id={}", id);
        return orderMapper.selectById(id);
    }

    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    public List<OrderInfo> getOrdersByUserId(Long userId) {
        logger.info("查询用户订单：userId={}", userId);
        return orderMapper.selectByUserId(userId);
    }

    /**
     * 创建订单
     * 使用雪花算法生成订单ID
     * @param userId 用户ID
     * @param productId 商品ID
     * @param seckillProductId 秒杀商品ID
     * @return 创建后的订单对象
     */
    public OrderInfo createOrder(Long userId, Long productId, Long seckillProductId) {
        OrderInfo order = new OrderInfo();
        order.setId(snowflakeIdGenerator.nextId());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setSeckillProductId(seckillProductId);
        order.setStatus(0);
        order.setCreateTime(new Date());
        
        orderMapper.insert(order);
        logger.info("创建订单：orderId={}, userId={}, productId={}", 
            order.getId(), userId, productId);
        
        return order;
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态
     * @return 更新后的订单对象
     */
    public OrderInfo updateOrderStatus(Long id, Integer status) {
        orderMapper.updateStatus(id, status);
        OrderInfo order = orderMapper.selectById(id);
        logger.info("更新订单状态：orderId={}, status={}", id, status);
        return order;
    }

    /**
     * 检查用户是否已购买该商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return true表示已购买，false表示未购买
     */
    public boolean checkUserHasOrder(Long userId, Long productId) {
        OrderInfo order = orderMapper.selectByUserIdAndProductId(userId, productId);
        return order != null;
    }
}