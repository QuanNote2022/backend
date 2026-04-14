-- 登录设备和历史记录初始化脚本
-- 执行此脚本前，请确保已执行基础数据库初始化

USE mineral_system;

-- 登录设备表
DROP TABLE IF EXISTS login_devices;
CREATE TABLE login_devices (
  device_id VARCHAR(64) PRIMARY KEY COMMENT '设备唯一标识',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  device_type VARCHAR(20) DEFAULT NULL COMMENT '设备类型：desktop/mobile/tablet',
  os VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  browser VARCHAR(50) DEFAULT NULL COMMENT '浏览器信息',
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  is_current TINYINT(1) DEFAULT 0 COMMENT '是否为当前设备',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id),
  INDEX idx_user_current (user_id, is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备表';

-- 登录历史表
DROP TABLE IF EXISTS login_history;
CREATE TABLE login_history (
  history_id VARCHAR(64) PRIMARY KEY COMMENT '记录 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  status VARCHAR(20) DEFAULT 'success' COMMENT '登录状态：success/failed',
  location VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id),
  INDEX idx_user_time (user_id, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录历史表';