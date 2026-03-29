-- 矿物识别系统数据库初始化脚本
-- 数据库：mineral_system

-- 创建数据库
CREATE DATABASE IF NOT EXISTS mineral_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mineral_system;

-- 用户表
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  user_id VARCHAR(64) PRIMARY KEY COMMENT '用户 ID',
  username VARCHAR(32) UNIQUE NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  email VARCHAR(128) UNIQUE NOT NULL COMMENT '邮箱',
  avatar VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
  nickname VARCHAR(32) DEFAULT NULL COMMENT '昵称',
  is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记',
  INDEX idx_username (username),
  INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 识别记录表
DROP TABLE IF EXISTS detections;
CREATE TABLE detections (
  detect_id VARCHAR(64) PRIMARY KEY COMMENT '识别记录 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  image_url VARCHAR(512) NOT NULL COMMENT '图片 URL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记',
  INDEX idx_user_id (user_id),
  INDEX idx_user_created (user_id, created_at),
  INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='矿物识别记录表';

-- 识别结果表
DROP TABLE IF EXISTS detection_results;
CREATE TABLE detection_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
  detect_id VARCHAR(64) NOT NULL COMMENT '识别记录 ID',
  label VARCHAR(64) NOT NULL COMMENT '矿物标签',
  confidence DECIMAL(5,4) NOT NULL COMMENT '置信度',
  bbox_x1 INT DEFAULT NULL COMMENT '边界框 x1',
  bbox_y1 INT DEFAULT NULL COMMENT '边界框 y1',
  bbox_x2 INT DEFAULT NULL COMMENT '边界框 x2',
  bbox_y2 INT DEFAULT NULL COMMENT '边界框 y2',
  INDEX idx_detect_id (detect_id),
  INDEX idx_label (label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='矿物识别结果表';

-- 矿物信息表
DROP TABLE IF EXISTS minerals;
CREATE TABLE minerals (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
  name VARCHAR(64) UNIQUE NOT NULL COMMENT '矿物名称',
  formula VARCHAR(128) DEFAULT NULL COMMENT '化学式',
  hardness VARCHAR(16) DEFAULT NULL COMMENT '硬度',
  luster VARCHAR(32) DEFAULT NULL COMMENT '光泽',
  color VARCHAR(64) DEFAULT NULL COMMENT '颜色',
  origin VARCHAR(256) DEFAULT NULL COMMENT '产地',
  uses VARCHAR(512) DEFAULT NULL COMMENT '用途',
  description TEXT COMMENT '描述',
  thumbnail VARCHAR(512) DEFAULT NULL COMMENT '缩略图 URL',
  INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='矿物信息表';

-- 聊天会话表
DROP TABLE IF EXISTS chat_sessions;
CREATE TABLE chat_sessions (
  session_id VARCHAR(64) PRIMARY KEY COMMENT '会话 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  title VARCHAR(128) NOT NULL COMMENT '会话标题',
  mineral_name VARCHAR(64) DEFAULT NULL COMMENT '矿物名称',
  detect_id VARCHAR(64) DEFAULT NULL COMMENT '关联的识别记录 ID',
  message_count INT DEFAULT 0 COMMENT '消息数量',
  last_active_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记',
  INDEX idx_user_id (user_id),
  INDEX idx_user_last_active (user_id, last_active_at),
  INDEX idx_detect_id (detect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 聊天消息表
DROP TABLE IF EXISTS chat_messages;
CREATE TABLE chat_messages (
  message_id VARCHAR(64) PRIMARY KEY COMMENT '消息 ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话 ID',
  role ENUM('user', 'assistant') NOT NULL COMMENT '角色',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_session_id (session_id),
  INDEX idx_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 初始化矿物数据
INSERT INTO minerals (name, formula, hardness, luster, color, origin, uses, description, thumbnail) VALUES
('石英', 'SiO₂', '7', '玻璃光泽', '无色/白色/粉色/紫色', '火成岩、变质岩、沉积岩', '玻璃制造、建筑材料、电子工业', '石英是一种常见的矿物，主要成分是二氧化硅。它具有良好的压电性和光学性能，在自然界中分布广泛。', '/thumbnails/quartz.jpg'),
('长石', '(K,Na,Ca)(Al,Si)₄O₈', '6-6.5', '玻璃光泽', '肉红色/灰白色/白色', '火成岩、变质岩', '陶瓷工业、玻璃工业、建筑材料', '长石是地壳中含量最多的矿物，约占地壳重量的 60%。主要用于陶瓷和玻璃工业。', '/thumbnails/feldspar.jpg'),
('云母', 'KAl₂(AlSi₃O₁₀)(OH)₂', '2-3', '珍珠光泽', '无色/黑色/绿色/棕色', '火成岩、变质岩', '电子工业、绝缘材料、涂料', '云母具有良好的绝缘性和耐热性，片状结构，易剥离成薄片。', '/thumbnails/mica.jpg'),
('方解石', 'CaCO₃', '3', '玻璃光泽', '无色/白色/灰色/黄色', '沉积岩、变质岩', '建筑材料、冶金熔剂、化工原料', '方解石是石灰岩和大理岩的主要成分，遇稀盐酸剧烈起泡。', '/thumbnails/calcite.jpg'),
('角闪石', '(Ca,Na)₂-₃(Mg,Fe,Al)₅(Si₆Al)₂O₂₂(OH)₂', '5-6', '玻璃光泽', '黑色/绿色/棕色', '火成岩、变质岩', '建筑材料', '角闪石是常见的造岩矿物，呈长柱状或针状晶体。', '/thumbnails/amphibole.jpg'),
('橄榄石', '(Mg,Fe)₂SiO₄', '6.5-7', '玻璃光泽', '橄榄绿色/黄绿色', '火成岩', '耐火材料、铸造砂、宝石', '橄榄石是上地幔的主要矿物成分，常见于玄武岩和橄榄岩中。', '/thumbnails/olivine.jpg'),
('辉石', '(Ca,Na)(Mg,Fe,Al)(Si,Al)₂O₆', '5-6.5', '玻璃光泽', '黑色/绿色/褐色', '火成岩、变质岩', '建筑材料', '辉石是重要的造岩矿物，短柱状晶体，常见于火成岩中。', '/thumbnails/pyroxene.jpg'),
('黄铁矿', 'FeS₂', '6-6.5', '金属光泽', '浅铜黄色', '火成岩、沉积岩、变质岩', '制硫酸、提取铁', '黄铁矿因其浅铜黄色和金属光泽被称为"愚人金"，是制硫酸的重要原料。', '/thumbnails/pyrite.jpg'),
('磁铁矿', 'Fe₃O₄', '5.5-6.5', '半金属光泽', '铁黑色', '火成岩、变质岩', '炼铁原料', '磁铁矿具有强磁性，是最重要的铁矿石矿物。', '/thumbnails/magnetite.jpg'),
('赤铁矿', 'Fe₂O₃', '5.5-6.5', '半金属光泽', '钢灰色/红棕色', '沉积岩、变质岩', '炼铁原料、颜料', '赤铁矿是最重要的铁矿石之一，条痕呈樱桃红色。', '/thumbnails/hematite.jpg');

-- 插入测试用户（密码：123456）
INSERT INTO users (user_id, username, password_hash, email, nickname, is_active) VALUES
('usr_test001', 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDJfLmS1N7K9fKZJ5vLJhQJZqP3G', 'test@example.com', '测试用户', 1);
