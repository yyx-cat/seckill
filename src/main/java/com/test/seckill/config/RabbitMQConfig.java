package com.test.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 * 包含主队列、死信队列和消息确认配置
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 交换机名称
     */
    public static final String EXCHANGE_NAME = "seckill.order.exchange";
    
    /**
     * 队列名称
     */
    public static final String QUEUE_NAME = "seckill.order.queue";
    
    /**
     * 路由键
     */
    public static final String ROUTING_KEY = "order";

    /**
     * 死信交换机名称
     */
    public static final String DEAD_LETTER_EXCHANGE_NAME = "seckill.order.dlx.exchange";
    
    /**
     * 死信队列名称
     */
    public static final String DEAD_LETTER_QUEUE_NAME = "seckill.order.dlx.queue";
    
    /**
     * 死信路由键
     */
    public static final String DEAD_LETTER_ROUTING_KEY = "order.dlx";

    /**
     * 消息过期时间（毫秒）- 5分钟
     */
    public static final long MESSAGE_TTL = 300000;

    /**
     * 创建交换机
     * @return DirectExchange实例
     */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * 创建死信交换机
     * @return DirectExchange实例
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE_NAME, true, false);
    }

    /**
     * 创建队列（绑定死信交换机）
     * @return Queue实例
     */
    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        args.put("x-message-ttl", MESSAGE_TTL);
        
        return new Queue(QUEUE_NAME, true, false, false, args);
    }

    /**
     * 创建死信队列
     * @return Queue实例
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE_NAME, true, false, false);
    }

    /**
     * 绑定队列到交换机
     * @return Binding实例
     */
    @Bean
    public Binding binding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(ROUTING_KEY);
    }

    /**
     * 绑定死信队列到死信交换机
     * @return Binding实例
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 配置消息转换器，使用JSON格式
     * @return Jackson2JsonMessageConverter实例
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate，设置消息转换器和确认回调
     * @param connectionFactory 连接工厂
     * @return RabbitTemplate实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                logger.info("消息发送成功，correlationId={}", 
                    correlationData != null ? correlationData.getId() : "null");
            } else {
                logger.error("消息发送失败，correlationId={}, cause={}", 
                    correlationData != null ? correlationData.getId() : "null", cause);
            }
        });
        
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            logger.error("消息返回，exchange={}, routingKey={}, replyCode={}, replyText={}, message={}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText(),
                returnedMessage.getMessage());
        });
        
        return rabbitTemplate;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class);
}