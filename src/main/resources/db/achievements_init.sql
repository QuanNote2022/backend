-- 成就系统初始化脚本
-- 执行此脚本前，请确保已执行基础数据库初始化

USE mineral_system;

-- 成就表
DROP TABLE IF EXISTS achievements;
CREATE TABLE achievements (
  achievement_id VARCHAR(64) PRIMARY KEY COMMENT '成就 ID',
  name VARCHAR(100) NOT NULL COMMENT '成就名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '成就描述',
  icon VARCHAR(50) DEFAULT NULL COMMENT '成就图标（emoji或图片URL）',
  level INT DEFAULT 1 COMMENT '成就等级（1-3）',
  achievement_type VARCHAR(50) DEFAULT NULL COMMENT '成就类型',
  target_value INT DEFAULT 0 COMMENT '目标值',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_type (achievement_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

-- 用户成就表
DROP TABLE IF EXISTS user_achievements;
CREATE TABLE user_achievements (
  id VARCHAR(64) PRIMARY KEY COMMENT '主键 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  achievement_id VARCHAR(64) NOT NULL COMMENT '成就 ID',
  unlocked TINYINT(1) DEFAULT 0 COMMENT '是否已解锁',
  unlocked_at DATETIME DEFAULT NULL COMMENT '解锁时间',
  progress INT DEFAULT 0 COMMENT '当前进度',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (achievement_id) REFERENCES achievements(achievement_id) ON DELETE CASCADE,
  UNIQUE KEY uk_user_achievement (user_id, achievement_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

-- 用户统计表
DROP TABLE IF EXISTS user_stats;
CREATE TABLE user_stats (
  user_id VARCHAR(64) PRIMARY KEY COMMENT '用户 ID',
  total_detections INT DEFAULT 0 COMMENT '总识别次数',
  total_chats INT DEFAULT 0 COMMENT '总问答次数',
  active_days INT DEFAULT 0 COMMENT '活跃天数',
  consecutive_days INT DEFAULT 0 COMMENT '连续登录天数',
  top_mineral VARCHAR(100) DEFAULT NULL COMMENT '最常识别的矿物',
  mineral_types INT DEFAULT 0 COMMENT '识别矿物种类数',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';

-- 初始化成就数据
INSERT INTO achievements (achievement_id, name, description, icon, level, achievement_type, target_value) VALUES
('first_detect', '初识矿物', '完成第一次矿物识别', '🎯', 1, 'detect', 1),
('detect_100', '识别达人', '累计识别100次矿物', '🏆', 2, 'detect', 100),
('detect_500', '识别大师', '累计识别500次矿物', '💎', 3, 'detect', 500),
('mineral_10', '探索者', '识别10种不同的矿物', '🔬', 1, 'mineral', 10),
('mineral_50', '矿物大师', '识别50种不同的矿物', '🏅', 3, 'mineral', 50),
('chat_50', '问答新星', '完成50次科普问答', '💬', 1, 'chat', 50),
('chat_200', '矿物百科', '完成200次科普问答', '📚', 2, 'chat', 200),
('login_7', '活跃用户', '连续登录7天', '🔥', 1, 'login', 7),
('login_30', '坚持不懈', '连续登录30天', '⭐', 2, 'login', 30);