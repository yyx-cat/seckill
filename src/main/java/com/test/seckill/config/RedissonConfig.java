package com.test.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Redisson配置类
 * 支持单机模式和集群模式
 * 根据配置自动切换连接方式
 */
@Configuration
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${spring.data.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    /**
     * 创建Redisson客户端
     * 根据配置自动选择单机模式或集群模式
     * @return RedissonClient实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        if (clusterEnabled && clusterNodes != null && !clusterNodes.isEmpty()) {
            configureClusterMode(config);
        } else {
            configureSingleMode(config);
        }

        RedissonClient client = Redisson.create(config);
        
        if (clusterEnabled) {
            logger.info("Redisson客户端初始化成功（集群模式）：nodes={}", clusterNodes);
        } else {
            logger.info("Redisson客户端初始化成功（单机模式）：redis://{}:{}", redisHost, redisPort);
        }
        
        return client;
    }

    /**
     * 配置单机模式
     * @param config Redisson配置
     */
    private void configureSingleMode(Config config) {
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(100)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(30000)
                .setConnectTimeout(10000)
                .setTimeout(3000);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }
    }

    /**
     * 配置集群模式
     * @param config Redisson配置
     */
    private void configureClusterMode(Config config) {
        java.util.Set<String> nodeAddresses = new java.util.HashSet<>();
        for (String node : clusterNodes) {
            nodeAddresses.add("redis://" + node);
        }

        ClusterServersConfig clusterConfig = config.useClusterServers()
                .addNodeAddress(nodeAddresses.toArray(new String[0]))
                .setMasterConnectionPoolSize(50)
                .setSlaveConnectionPoolSize(50)
                .setMasterConnectionMinimumIdleSize(10)
                .setSlaveConnectionMinimumIdleSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setIdleConnectionTimeout(30000)
                .setScanInterval(2000);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            clusterConfig.setPassword(redisPassword);
        }
    }
}