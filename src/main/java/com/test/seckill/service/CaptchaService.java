package com.test.seckill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * 生成和验证图形验证码
 * 防止机器人恶意请求
 */
@Service
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    /**
     * 验证码key前缀
     */
    private static final String CAPTCHA_KEY = "seckill:captcha:";

    /**
     * 验证码有效期（分钟）
     */
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 验证码字符集
     */
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 验证码长度
     */
    private static final int CAPTCHA_LENGTH = 4;

    /**
     * 图片宽度
     */
    private static final int WIDTH = 100;

    /**
     * 图片高度
     */
    private static final int HEIGHT = 40;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();

    /**
     * 生成验证码图片
     * @param userId 用户ID
     * @return 验证码图片字节数组（PNG格式）
     */
    public byte[] generateCaptcha(Long userId) {
        // 生成验证码字符
        String captcha = generateCaptchaString();
        
        // 存储到Redis
        String key = CAPTCHA_KEY + userId;
        redisTemplate.opsForValue().set(key, captcha, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 生成图片
        BufferedImage image = createCaptchaImage(captcha);
        
        // 转换为字节数组
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", outputStream);
            logger.info("生成验证码：userId={}, captcha={}", userId, captcha);
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("生成验证码图片失败", e);
            return null;
        }
    }

    /**
     * 验证验证码
     * @param userId 用户ID
     * @param captcha 用户输入的验证码
     * @return true表示验证通过
     */
    public boolean validateCaptcha(Long userId, String captcha) {
        String key = CAPTCHA_KEY + userId;
        Object storedCaptcha = redisTemplate.opsForValue().get(key);
        
        if (storedCaptcha == null) {
            logger.warn("验证码已过期：userId={}", userId);
            return false;
        }

        boolean valid = storedCaptcha.toString().equalsIgnoreCase(captcha);
        
        if (valid) {
            // 验证通过后删除验证码（防止重复使用）
            redisTemplate.delete(key);
            logger.info("验证码验证通过：userId={}", userId);
        } else {
            logger.warn("验证码验证失败：userId={}, expected={}, actual={}", 
                userId, storedCaptcha, captcha);
        }
        
        return valid;
    }

    /**
     * 生成验证码字符
     * @return 验证码字符串
     */
    private String generateCaptchaString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * 创建验证码图片
     * @param captcha 验证码字符
     * @return BufferedImage对象
     */
    private BufferedImage createCaptchaImage(String captcha) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 设置字体
        g.setFont(new Font("Arial", Font.BOLD, 28));

        // 绘制验证码字符
        char[] chars = captcha.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            // 随机颜色
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            // 随机位置和角度
            int x = 15 + i * 20;
            int y = 28;
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(chars[i]), x, y);
            g.rotate(-angle, x, y);
        }

        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), 
                random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // 绘制干扰点
        for (int i = 0; i < 20; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();
        return image;
    }
}