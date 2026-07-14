-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seckill;

-- 商品主表（基础商品信息）
CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT DEFAULT 0 COMMENT '普通库存（第一阶段暂不使用）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀商品表（与活动强相关）
CREATE TABLE IF NOT EXISTS seckill_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seckill_price DECIMAL(10,2) NOT NULL,
  seckill_stock INT NOT NULL COMMENT '秒杀可用库存',
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status TINYINT DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已结束',
  version INT DEFAULT 0 COMMENT '乐观锁版本号（为后续预留）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(id),
  INDEX idx_product_id (product_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS order_info (
  id BIGINT PRIMARY KEY COMMENT '订单ID（雪花算法生成）',
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  seckill_product_id BIGINT COMMENT '关联秒杀商品ID',
  status TINYINT DEFAULT 0 COMMENT '0-未支付 1-已支付',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 本地消息表（用于实现可靠消息最终一致性）
CREATE TABLE IF NOT EXISTS local_message (
  id BIGINT PRIMARY KEY COMMENT '消息ID（雪花算法生成）',
  business_type VARCHAR(50) NOT NULL COMMENT '业务类型：SECKILL-秒杀订单',
  business_id VARCHAR(100) NOT NULL COMMENT '业务ID（格式：userId:productId）',
  message_content TEXT COMMENT '消息内容（JSON格式）',
  status TINYINT DEFAULT 0 COMMENT '0-待确认 1-已确认 2-已补偿',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
  next_retry_time DATETIME COMMENT '下次重试时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_business_type (business_type),
  INDEX idx_status (status),
  INDEX idx_next_retry_time (next_retry_time),
  UNIQUE INDEX uk_business_type_id (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO product (name, price, stock) VALUES ('iPhone 15 Pro', 8999.00, 100);
INSERT INTO product (name, price, stock) VALUES ('MacBook Pro', 14999.00, 50);

INSERT INTO seckill_product (product_id, seckill_price, seckill_stock, start_time, end_time, status) 
VALUES (1, 5999.00, 10, NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1);

INSERT INTO seckill_product (product_id, seckill_price, seckill_stock, start_time, end_time, status) 
VALUES (2, 9999.00, 5, NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1);