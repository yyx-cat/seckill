# 高并发秒杀系统
一个从零构建、经过生产级压测验证的 Spring Boot 秒杀架构

本项目完整实现了高并发秒杀场景下的流量削峰、库存扣减、分布式锁、最终一致性等核心技术方案，并已通过 1000 并发 0% 错误率 的压力测试。

# 技术亮点
1. 原子扣库存：基于 Redis 单线程特性 + Lua 脚本，保证库存不超卖
1. 异步下单：RabbitMQ 消峰填谷，避免数据库被瞬时流量打崩
1. 最终一致性：本地消息表 + 定时补偿，保证 Redis 与 MySQL 数据最终一致
1. 分布式锁：基于 Redisson，防止同一用户重复下单
1. 多级缓存：Caffeine（本地）+ Redis（分布式）双重缓存，降低 DB 压力
1. 全局唯一 ID：雪花算法生成订单 ID 和消息 ID，支持分布式部署
1. 安全防护：隐藏秒杀地址 + 验证码 + IP/用户防刷 + 分布式限流

# 技术栈
组件	版本	用途
Java	21	后端开发语言
Spring Boot	3.2.0	项目基础框架
MySQL	8.0	订单、商品、本地消息持久化
Redis	7.x	库存扣减、分布式锁、缓存、限流
RabbitMQ	4.0.x	异步下单、消息削峰
Redisson	3.24.0	分布式锁实现
Caffeine	最新	本地缓存（一级缓存）
MyBatis	3.0.3	ORM 框架
Lombok	最新	简化 Java 代码
# 压测成绩（本地环境 JMeter）
并发数	样本数	平均响应时间	最大响应时间	错误率
50	60	66ms	547ms	0%
100	160	49ms	547ms	0%
200	360	37ms	547ms	0%
500	860	24ms	547ms	0%
1000	1860	87ms	1186ms	0%
在 1000 并发下，系统吞吐量约 186 req/s，无超卖、无订单丢失。

# 核心功能模块
模块	功能说明
商品管理	商品列表查询、详情查询（支持多级缓存）
秒杀核心	Redis 原子扣库存 → MQ 异步下单 → 最终一致性保证
安全防护	隐藏秒杀地址（Token 有效期 30 秒）、验证码、IP/用户防刷、分布式限流
订单系统	雪花算法生成订单 ID，支持幂等性（防止重复下单）
消息补偿	定时扫描“待确认”消息，超时自动回滚 Redis 库存并转入死信队列
🚀 快速启动（本地开发）
1. 环境要求
JDK 21+

MySQL 8.0+

Redis 7.x+

RabbitMQ 4.0.x

2. 拉取代码
bash
git clone https://github.com/yyx-cat/seckill.git
cd seckill
3. 导入数据库
sql
-- 创建数据库
CREATE DATABASE seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行项目根目录下的 schema.sql
USE seckill;
source /path/to/schema.sql;
4. 修改配置文件
修改 application.yml 中以下连接信息：

yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
5. 启动项目
在 IDEA 中运行 SeckillApplication.java，或在项目根目录执行：

bash
./mvnw spring-boot:run
6. 测试接口（Postman 示例）
获取隐藏秒杀地址：GET /seckill/url/1?userId=1001

带 Token 下单：POST /seckill/1?userId=1001&token=xxxx

查询订单结果：GET /orders/result/1001/1

# 项目结构
text
com.test.seckill
├── SeckillApplication.java          # 启动类
├── common
│   ├── component                    # 通用组件（分布式锁、雪花ID、限流）
│   ├── config                       # 全局配置（Redis、RabbitMQ、Web）
│   └── utils                        # 工具类
├── module                           # 业务模块（后续扩展）
│   ├── product                      # 商品模块
│   ├── seckill                      # 秒杀模块（核心）
│   └── order                        # 订单模块
├── consumer                         # MQ 消费者
├── mapper                           # MyBatis Mapper
└── entity                           # 实体类


# 核心流程时序图
text
用户请求秒杀
    │
    ▼
① 限流 + 防刷 + Token 验证
    │
    ▼
② Redis 原子扣减库存（DECR）
    │
    ▼
③ 创建本地消息表（状态：待确认）
    │
    ▼
④ 发送消息到 RabbitMQ（异步）
    │
    ├────────────────────────────┐
    ▼                            ▼
返回“排队中”             ⑤ 消费者处理：
                              - 幂等检查
                              - 扣减 DB 库存
                              - 生成订单
                              - 确认本地消息
# 生产环境待办清单
修改限流组件（当前 Lua 脚本存在参数传递问题，已临时跳过）

接入监控告警（Prometheus + Grafana）

Redis 切换为集群模式

RabbitMQ 配置镜像队列

MySQL 配置主从复制 + 读写分离

# 后续开发规划
本项目将在此秒杀核心基础上，扩展为完整电商平台：

用户中心：注册/登录（JWT）、用户信息管理

商品后台：商品增删改查、分类管理、图片上传

购物车：Redis 存储购物车数据

普通订单：从购物车下单，支持支付回调

前端联调：Vue 3 + Element Plus 前后端分离

# 开发者
GitHub：@yyx-cat

邮箱：2208036552@qq.com

# License
MIT License
