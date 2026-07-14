package com.test.seckill.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理Controller层抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        logger.warn("业务异常：{}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", e.getCode());
        response.put("message", e.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理通用异常
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        logger.error("系统异常", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("message", "系统繁忙，请稍后重试");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 业务异常类
     */
    public static class BusinessException extends RuntimeException {
        
        private int code;
        
        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }
        
        public BusinessException(String message) {
            super(message);
            this.code = 400;
        }
        
        public int getCode() {
            return code;
        }
    }
}