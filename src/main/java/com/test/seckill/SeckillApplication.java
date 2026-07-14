package com.test.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀系统启动类
 */
@SpringBootApplication
@EnableScheduling
public class SeckillApplication {

    /**
     * 主方法，启动Spring Boot应用
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}