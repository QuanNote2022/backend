-- 用户偏好设置初始化脚本
-- 执行此脚本前，请确保已执行基础数据库初始化

USE mineral_system;

-- 用户偏好设置表
DROP TABLE IF EXISTS user_preferences;
CREATE TABLE user_preferences (
  user_id VARCHAR(64) PRIMARY KEY COMMENT '用户 ID',
  email_notification TINYINT(1) DEFAULT 1 COMMENT '邮件通知开关',
  system_notification TINYINT(1) DEFAULT 1 COMMENT '系统通知开关',
  theme VARCHAR(20) DEFAULT 'light' COMMENT '界面主题：light/dark/auto',
  language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言：zh-CN/en-US',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';