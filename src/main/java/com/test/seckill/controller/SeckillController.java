package com.test.seckill.controller;

import com.test.seckill.component.AntiBrushComponent;
import com.test.seckill.component.RateLimitComponent;
import com.test.seckill.entity.SeckillProduct;
import com.test.seckill.service.CaptchaService;
import com.test.seckill.service.SeckillService;
import com.test.seckill.service.SeckillUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀控制器
 * 提供秒杀相关接口
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final Logger logger = LoggerFactory.getLogger(SeckillController.class);

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private SeckillUrlService seckillUrlService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RateLimitComponent rateLimitComponent;

    @Autowired
    private AntiBrushComponent antiBrushComponent;

    /**
     * 获取秒杀隐藏地址
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 隐藏地址token
     */
    @GetMapping("/url")
    public ResponseEntity<Map<String, Object>> getSeckillUrl(
            @RequestParam Long userId, 
            @RequestParam Long productId) {
        
        logger.info("获取秒杀隐藏地址：userId={}, productId={}", userId, productId);
        
        String token = seckillUrlService.generateSeckillUrl(userId, productId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "获取成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取验证码
     * @param userId 用户ID
     * @return 验证码图片
     */
    @GetMapping("/captcha")
    public ResponseEntity<byte[]> getCaptcha(@RequestParam Long userId) {
        logger.info("获取验证码：userId={}", userId);
        
        byte[] captcha = captchaService.generateCaptcha(userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        
        return ResponseEntity.ok().headers(headers).body(captcha);
    }

    /**
     * 执行秒杀
     * @param token 隐藏地址token
     * @param userId 用户ID
     * @param productId 商品ID
     * @param captcha 验证码
     * @param request HTTP请求
     * @return 秒杀结果
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeSeckill(
            @RequestParam String token,
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam String captcha,
            HttpServletRequest request) {
        
        logger.info("执行秒杀：userId={}, productId={}", userId, productId);
        
        Map<String, Object> response = new HashMap<>();
        
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 1. 防刷检查
        if (!antiBrushComponent.checkAccess(clientIp, userId)) {
            response.put("code", 403);
            response.put("message", "请求过于频繁，请稍后重试");
            return ResponseEntity.status(403).body(response);
        }
        
        // 2. 限流检查
        if (!rateLimitComponent.checkAll(userId, clientIp)) {
            response.put("code", 429);
            response.put("message", "系统繁忙，请稍后重试");
            return ResponseEntity.status(429).body(response);
        }
        
        // 3. 验证隐藏地址
        if (!seckillUrlService.validateSeckillUrl(token, userId, productId)) {
            response.put("code", 400);
            response.put("message", "秒杀地址无效或已过期");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 4. 验证验证码
        if (!captchaService.validateCaptcha(userId, captcha)) {
            response.put("code", 400);
            response.put("message", "验证码错误或已过期");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 5. 执行秒杀
        boolean success = seckillService.seckill(userId, productId);
        
        if (success) {
            response.put("code", 200);
            response.put("message", "秒杀成功，订单正在处理中");
            return ResponseEntity.ok(response);
        } else {
            response.put("code", 400);
            response.put("message", "秒杀失败，请重试");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 查询秒杀商品信息
     * @param productId 商品ID
     * @return 秒杀商品信息
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<SeckillProduct> getSeckillProduct(@PathVariable Long productId) {
        logger.info("查询秒杀商品：productId={}", productId);
        
        SeckillProduct product = seckillService.getSeckillProduct(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(product);
    }

    /**
     * 获取客户端IP地址
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多个代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}