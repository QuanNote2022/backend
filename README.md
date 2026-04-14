# 矿物识别系统

一个基于 Spring Boot 3.2 + Vue 3 的矿物识别系统，集成 YOLO 目标检测和 AI 大语言模型，提供矿物识别、科普问答、历史记录管理等功能。

## 目录

- [系统简介](#系统简介)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [API 接口文档](#api-接口文档)
- [数据库设计](#数据库设计)
- [表关系](#表关系)
- [功能介绍](#功能介绍)
- [部署说明](#部署说明)

---

## 系统简介

矿物识别系统是一个智能化的矿物识别和科普平台，主要功能包括：

- **矿物识别**：上传矿物图片，通过 AI 模型自动识别矿物种类
- **智能问答**：基于大语言模型的矿物科普问答
- **历史记录**：管理识别历史和聊天历史
- **数据统计**：个人使用统计和成就系统
- **用户管理**：完善的用户资料和偏好设置

---

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **安全**: Spring Security + JWT
- **ORM**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0+
- **工具**: Lombok, Hutool
- **构建工具**: Maven
- **AI 集成**: Ollama (大语言模型)

### 前端
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI 组件**: Naive UI
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP 客户端**: Axios

### AI 模型
- **目标检测**: YOLOv8
- **大语言模型**: Ollama (支持 Qwen、ChatGLM 等)

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+
- Ollama (可选，用于 AI 问答)

### 1. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source backend/src/main/resources/db/init.sql
source backend/src/main/resources/db/achievements_init.sql
source backend/src/main/resources/db/user_preferences_init.sql
source backend/src/main/resources/db/login_devices_init.sql
```

### 2. 后端配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mineral_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

ollama:
  api-host: http://localhost:11434
  chat-model: qwen:7b
```

### 3. 后端运行

```bash
cd backend
mvn clean package
java -jar target/mineral-system-1.0.0.jar
```

### 4. 前端配置

编辑 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 5. 前端运行

```bash
cd frontend
npm install
npm run dev
```

### 6. 访问系统

- 前端地址：http://localhost:5173
- 后端地址：http://localhost:8080/api

---

## 项目结构

```
mineral-system/
├── backend/                        # 后端项目
│   ├── src/main/java/com/mineral/
│   │   ├── common/                 # 通用类
│   │   │   ├── ApiResponse.java    # 统一响应
│   │   │   ├── PageResult.java     # 分页结果
│   │   │   ├── PageQuery.java      # 分页查询
│   │   │   ├── ErrorCode.java      # 错误码
│   │   │   ├── BusinessException.java # 业务异常
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── config/                 # 配置类
│   │   │   ├── CorsConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtUtil.java
│   │   │   └── MybatisPlusConfig.java
│   │   ├── controller/             # 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── MineralController.java
│   │   │   ├── ChatController.java
│   │   │   ├── HistoryController.java
│   │   │   └── StatsController.java
│   │   ├── dto/                    # 数据传输对象
│   │   ├── entity/                 # 实体类
│   │   ├── mapper/                 # Mapper 接口
│   │   └── service/                # 服务层
│   └── src/main/resources/
│       ├── application.yml
│       └── db/                     # 数据库初始化脚本
├── frontend/                       # 前端项目
│   ├── src/
│   │   ├── api/                    # API 接口
│   │   ├── components/             # 组件
│   │   ├── router/                 # 路由
│   │   ├── stores/                 # 状态管理
│   │   ├── types/                  # TypeScript 类型
│   │   ├── utils/                  # 工具函数
│   │   └── views/                  # 页面
│   └── package.json
└── yolo/                           # YOLO 模型相关
    ├── dataset/                    # 数据集
    └── train.py                    # 训练脚本
```

---

## API 接口文档

### 统一响应格式

所有接口返回统一使用以下格式：

```typescript
interface ApiResponse<T> {
  code: number      // 状态码，0 表示成功
  message: string   // 响应消息
  data: T          // 响应数据
}
```

### 认证模块

#### 1. 用户注册
- **接口**: `POST /api/auth/register`
- **描述**: 注册新用户
- **请求参数**:
  ```json
  {
    "username": "string",     // 用户名，必填，3-20 个字符
    "password": "string",     // 密码，必填，6-20 个字符
    "email": "string"         // 邮箱，必填，有效邮箱格式
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 0,
    "message": "注册成功",
    "data": {
      "userId": "usr_123456789"
    }
  }
  ```

#### 2. 用户登录
- **接口**: `POST /api/auth/login`
- **描述**: 用户登录获取 Token
- **请求参数**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 0,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 86400
    }
  }
  ```

#### 3. 用户登出
- **接口**: `POST /api/auth/logout`
- **描述**: 用户登出，使当前 Token 失效
- **请求头**: `Authorization: Bearer <token>`

---

### 用户模块

#### 1. 获取用户信息
- **接口**: `GET /api/user/profile`
- **描述**: 获取当前登录用户的详细信息
- **请求头**: `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "userId": "usr_123456789",
      "username": "zhangsan",
      "email": "zhangsan@example.com",
      "avatar": "https://example.com/avatars/usr_123456789.jpg",
      "nickname": "张三",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  }
  ```

#### 2. 更新用户信息
- **接口**: `PUT /api/user/profile`
- **描述**: 更新当前用户的基本信息
- **请求参数**:
  ```json
  {
    "email": "string",
    "avatar": "string",
    "nickname": "string"
  }
  ```

#### 3. 修改密码
- **接口**: `PUT /api/user/password`
- **描述**: 修改当前用户的登录密码
- **请求参数**:
  ```json
  {
    "oldPassword": "string",
    "newPassword": "string"
  }
  ```

#### 4. 获取用户统计
- **接口**: `GET /api/user/stats`
- **描述**: 获取用户的使用统计数据
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": {
      "totalDetections": 50,
      "totalChats": 30,
      "topMineral": "石英",
      "activeDays": 15,
      "consecutiveDays": 7,
      "mineralTypes": 8
    }
  }
  ```

#### 5. 获取用户成就
- **接口**: `GET /api/user/achievements`
- **描述**: 获取用户的成就列表

#### 6. 获取登录设备列表
- **接口**: `GET /api/user/devices`
- **描述**: 获取用户的登录设备列表

#### 7. 登出指定设备
- **接口**: `DELETE /api/user/devices/{deviceId}`
- **描述**: 强制登出指定设备

#### 8. 获取登录历史
- **接口**: `GET /api/user/login-history`
- **描述**: 获取用户的登录历史记录（分页）
- **参数**: `page`, `pageSize`

#### 9. 获取用户偏好设置
- **接口**: `GET /api/user/preferences`
- **描述**: 获取用户的偏好设置

#### 10. 更新用户偏好设置
- **接口**: `PUT /api/user/preferences`
- **描述**: 更新用户的偏好设置
- **请求参数**:
  ```json
  {
    "emailNotification": true,
    "systemNotification": true,
    "theme": "light",
    "language": "zh-CN"
  }
  ```

---

### 矿物识别模块

#### 1. 矿物识别
- **接口**: `POST /api/mineral/detect`
- **描述**: 上传矿物图片进行识别
- **Content-Type**: `multipart/form-data`
- **请求参数**:
  ```
  image: File    // 图片文件，必填，支持 JPG/PNG 格式，最大 10MB
  ```
- **响应示例**:
  ```json
  {
    "code": 0,
    "message": "识别成功",
    "data": {
      "detectId": "det_123456789",
      "imageUrl": "https://example.com/uploads/2024/01/01/xxx.jpg",
      "results": [
        {
          "label": "石英",
          "confidence": 0.95,
          "bbox": [100, 100, 300, 300],
          "mineralInfo": {
            "name": "石英",
            "formula": "SiO₂",
            "hardness": "7",
            "luster": "玻璃光泽",
            "color": "无色/白色",
            "origin": "火成岩、变质岩",
            "uses": "玻璃制造、建筑材料",
            "description": "石英是一种常见的矿物...",
            "thumbnail": "https://example.com/thumbnails/quartz.jpg"
          }
        }
      ],
      "createdAt": "2024-01-01T12:00:00Z"
    }
  }
  ```

#### 2. 获取识别详情
- **接口**: `GET /api/mineral/detect/{detectId}`
- **描述**: 根据 ID 获取矿物识别的详细信息

#### 3. 获取矿物分类列表
- **接口**: `GET /api/mineral/categories`
- **描述**: 获取所有矿物分类及其数量统计
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": [
      {
        "id": "cat_001",
        "name": "石英",
        "count": 15
      },
      {
        "id": "cat_002",
        "name": "长石",
        "count": 8
      }
    ]
  }
  ```

#### 4. 获取矿物详细信息
- **接口**: `GET /api/mineral/info/{mineralName}`
- **描述**: 根据矿物名称获取详细信息
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": {
      "name": "石英",
      "formula": "SiO₂",
      "hardness": "7",
      "luster": "玻璃光泽",
      "color": "无色/白色",
      "origin": "火成岩、变质岩",
      "uses": "玻璃制造、建筑材料",
      "description": "石英是一种常见的矿物，主要成分是二氧化硅...",
      "thumbnail": "https://example.com/thumbnails/quartz.jpg"
    }
  }
  ```

---

### 聊天会话模块

#### 1. 创建聊天会话
- **接口**: `POST /api/chat/session`
- **描述**: 创建新的聊天会话，可关联矿物识别记录
- **请求参数**:
  ```json
  {
    "detectId": "string",
    "mineralName": "string"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 0,
    "message": "创建成功",
    "data": {
      "sessionId": "sess_123456789",
      "title": "石英矿物咨询",
      "mineralName": "石英",
      "messageCount": 0,
      "lastActiveAt": "2024-01-01T12:00:00Z",
      "createdAt": "2024-01-01T12:00:00Z"
    }
  }
  ```

#### 2. 获取会话列表
- **接口**: `GET /api/chat/sessions`
- **描述**: 获取当前用户的聊天会话列表（分页）
- **参数**: `page`, `pageSize`

#### 3. 获取会话消息
- **接口**: `GET /api/chat/session/{sessionId}/messages`
- **描述**: 获取指定会话的所有消息
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": [
      {
        "messageId": "msg_001",
        "sessionId": "sess_123456789",
        "role": "user",
        "content": "石英的硬度是多少？",
        "createdAt": "2024-01-01T12:05:00Z"
      },
      {
        "messageId": "msg_002",
        "sessionId": "sess_123456789",
        "role": "assistant",
        "content": "石英的莫氏硬度为 7...",
        "createdAt": "2024-01-01T12:05:01Z"
      }
    ]
  }
  ```

#### 4. 发送消息
- **接口**: `POST /api/chat/session/{sessionId}/send`
- **描述**: 向指定会话发送消息（支持 SSE 流式响应）
- **Content-Type**: `application/json`
- **请求参数**:
  ```json
  {
    "content": "string",
    "mineralContext": "string"
  }
  ```
- **响应格式** (SSE 流式):
  ```
  data: {"token": "石", "done": false}
  
  data: {"token": "英", "done": false}
  
  data: {"done": true, "messageId": "msg_003"}
  ```

#### 5. 删除会话
- **接口**: `DELETE /api/chat/session/{sessionId}`
- **描述**: 删除指定的聊天会话

---

### 历史记录模块

#### 1. 获取识别历史
- **接口**: `GET /api/history/detections`
- **描述**: 获取用户的矿物识别历史记录（分页、筛选）
- **参数**: 
  - `page`: 页码
  - `pageSize`: 每页数量
  - `keyword`: 搜索关键词（矿物名称）
  - `startDate`: 开始日期
  - `endDate`: 结束日期

#### 2. 删除识别记录
- **接口**: `DELETE /api/history/detections/{detectId}`
- **描述**: 删除指定的矿物识别记录

#### 3. 获取聊天历史
- **接口**: `GET /api/history/chats`
- **描述**: 获取用户的聊天会话历史记录（分页）
- **参数**: `page`, `pageSize`

---

### 统计模块

#### 1. 获取统计概览
- **接口**: `GET /api/stats/overview`
- **描述**: 获取用户的使用统计概览数据
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": {
      "totalDetections": 50,
      "totalChats": 30,
      "topMineral": "石英",
      "weeklyActiveDays": 5
    }
  }
  ```

#### 2. 获取矿物识别频率
- **接口**: `GET /api/stats/mineral-frequency`
- **描述**: 获取用户矿物识别频率统计
- **参数**: `days` - 统计天数，默认 30 天
- **响应示例**:
  ```json
  {
    "code": 0,
    "data": [
      {
        "mineralName": "石英",
        "count": 15
      },
      {
        "mineralName": "长石",
        "count": 8
      }
    ]
  }
  ```

---

## 数据库设计

### 1. 用户表 (users)

```sql
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
```

**字段说明**:
- `user_id`: 用户唯一标识
- `username`: 用户名，用于登录
- `password_hash`: BCrypt 加密的密码
- `email`: 用户邮箱
- `avatar`: 头像 URL
- `nickname`: 用户昵称
- `is_active`: 账号是否激活
- `deleted`: 逻辑删除标志

---

### 2. 识别记录表 (detections)

```sql
CREATE TABLE detections (
  detect_id VARCHAR(64) PRIMARY KEY COMMENT '识别记录 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  image_url VARCHAR(512) NOT NULL COMMENT '图片 URL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记',
  INDEX idx_user_id (user_id),
  INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='矿物识别记录表';
```

**字段说明**:
- `detect_id`: 识别记录唯一标识
- `user_id`: 关联的用户 ID
- `image_url`: 上传图片的存储 URL
- `created_at`: 识别时间

---

### 3. 识别结果表 (detection_results)

```sql
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
```

**字段说明**:
- `detect_id`: 关联的识别记录 ID
- `label`: 识别出的矿物名称
- `confidence`: 置信度 (0-1)
- `bbox_x1, bbox_y1, bbox_x2, bbox_y2`: 边界框坐标

---

### 4. 矿物信息表 (minerals)

```sql
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
```

**字段说明**:
- `name`: 矿物名称
- `formula`: 化学分子式
- `hardness`: 莫氏硬度
- `luster`: 光泽类型
- `color`: 颜色
- `origin`: 产地和岩石类型
- `uses`: 用途
- `description`: 详细描述
- `thumbnail`: 缩略图 URL

---

### 5. 聊天会话表 (chat_sessions)

```sql
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
  INDEX idx_user_last_active (user_id, last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';
```

**字段说明**:
- `session_id`: 会话唯一标识
- `user_id`: 关联的用户 ID
- `title`: 会话标题
- `mineral_name`: 关联的矿物名称
- `detect_id`: 关联的识别记录 ID
- `message_count`: 消息数量
- `last_active_at`: 最后活跃时间

---

### 6. 聊天消息表 (chat_messages)

```sql
CREATE TABLE chat_messages (
  message_id VARCHAR(64) PRIMARY KEY COMMENT '消息 ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话 ID',
  role ENUM('user', 'assistant') NOT NULL COMMENT '角色',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_session_id (session_id),
  INDEX idx_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';
```

**字段说明**:
- `message_id`: 消息唯一标识
- `session_id`: 关联的会话 ID
- `role`: 消息角色（user/assistant）
- `content`: 消息内容

---

### 7. 用户统计表 (user_stats)

```sql
CREATE TABLE user_stats (
  user_id VARCHAR(64) PRIMARY KEY COMMENT '用户 ID',
  total_detections INT DEFAULT 0 COMMENT '总识别次数',
  total_chats INT DEFAULT 0 COMMENT '总问答次数',
  active_days INT DEFAULT 0 COMMENT '活跃天数',
  consecutive_days INT DEFAULT 0 COMMENT '连续登录天数',
  top_mineral VARCHAR(100) DEFAULT NULL COMMENT '最常识别的矿物',
  mineral_types INT DEFAULT 0 COMMENT '识别矿物种类数',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';
```

---

### 8. 成就表 (achievements)

```sql
CREATE TABLE achievements (
  achievement_id VARCHAR(64) PRIMARY KEY COMMENT '成就 ID',
  name VARCHAR(100) NOT NULL COMMENT '成就名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '成就描述',
  icon VARCHAR(50) DEFAULT NULL COMMENT '成就图标',
  level INT DEFAULT 1 COMMENT '成就等级（1-3）',
  achievement_type VARCHAR(50) DEFAULT NULL COMMENT '成就类型',
  target_value INT DEFAULT 0 COMMENT '目标值',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';
```

**预设成就**:
- `first_detect`: 初识矿物（完成第一次识别）
- `detect_100`: 识别达人（累计识别 100 次）
- `detect_500`: 识别大师（累计识别 500 次）
- `mineral_10`: 探索者（识别 10 种不同矿物）
- `mineral_50`: 矿物大师（识别 50 种不同矿物）
- `chat_50`: 问答新星（完成 50 次问答）
- `chat_200`: 矿物百科（完成 200 次问答）
- `login_7`: 活跃用户（连续登录 7 天）
- `login_30`: 坚持不懈（连续登录 30 天）

---

### 9. 用户成就表 (user_achievements)

```sql
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
  UNIQUE KEY uk_user_achievement (user_id, achievement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';
```

---

### 10. 用户偏好设置表 (user_preferences)

```sql
CREATE TABLE user_preferences (
  user_id VARCHAR(64) PRIMARY KEY COMMENT '用户 ID',
  email_notification TINYINT(1) DEFAULT 1 COMMENT '邮件通知开关',
  system_notification TINYINT(1) DEFAULT 1 COMMENT '系统通知开关',
  theme VARCHAR(20) DEFAULT 'light' COMMENT '界面主题：light/dark/auto',
  language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言：zh-CN/en-US',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';
```

---

### 11. 登录设备表 (login_devices)

```sql
CREATE TABLE login_devices (
  device_id VARCHAR(64) PRIMARY KEY COMMENT '设备唯一标识',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  device_type VARCHAR(20) DEFAULT NULL COMMENT '设备类型：desktop/mobile/tablet',
  os VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  browser VARCHAR(50) DEFAULT NULL COMMENT '浏览器信息',
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP 地址',
  is_current TINYINT(1) DEFAULT 0 COMMENT '是否为当前设备',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备表';
```

---

### 12. 登录历史表 (login_history)

```sql
CREATE TABLE login_history (
  history_id VARCHAR(64) PRIMARY KEY COMMENT '记录 ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户 ID',
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP 地址',
  status VARCHAR(20) DEFAULT 'success' COMMENT '登录状态：success/failed',
  location VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录历史表';
```

---

## 表关系

### 实体关系图

```
┌─────────────┐
│    users    │
│  (用户表)    │
└──────┬──────┘
       │
       ├──────────────────┬─────────────────┬──────────────────┬─────────────────┐
       │                  │                 │                  │                 │
       ▼                  ▼                 ▼                  ▼                 ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ detections  │   │chat_sessions│   │ user_stats  │   │user_        │   │login_       │
│ (识别记录)   │   │ (聊天会话)   │   │ (用户统计)   │   │achievements │   │devices      │
└──────┬──────┘   └──────┬──────┘   └─────────────┘   └─────────────┘   │(登录设备)   │
       │                 │                                              └─────────────┘
       ▼                 ▼
┌─────────────┐   ┌─────────────┐
│detection_   │   │chat_        │
│results      │   │messages     │
│(识别结果)    │   │(聊天消息)    │
└─────────────┘   └─────────────┘
```

### 关系说明

1. **users → detections**: 一对多关系
   - 一个用户可以有多条识别记录
   - 通过 `user_id` 关联

2. **users → chat_sessions**: 一对多关系
   - 一个用户可以有多个聊天会话
   - 通过 `user_id` 关联

3. **users → user_stats**: 一对一关系
   - 一个用户对应一条统计记录
   - 通过 `user_id` 关联

4. **users → user_achievements**: 一对多关系
   - 一个用户可以有多个成就记录
   - 通过 `user_id` 关联

5. **users → login_devices**: 一对多关系
   - 一个用户可以在多个设备登录
   - 通过 `user_id` 关联

6. **users → login_history**: 一对多关系
   - 一个用户有多条登录历史记录
   - 通过 `user_id` 关联

7. **achievements → user_achievements**: 一对多关系
   - 一个成就可以被多个用户解锁
   - 通过 `achievement_id` 关联

8. **detections → detection_results**: 一对多关系
   - 一条识别记录可以有多个识别结果
   - 通过 `detect_id` 关联

9. **chat_sessions → chat_messages**: 一对多关系
   - 一个会话可以有多条消息
   - 通过 `session_id` 关联
   - 级联删除

10. **chat_sessions → detections**: 多对一关系
    - 聊天会话可以关联到识别记录
    - 通过 `detect_id` 关联

---

## 功能介绍

### 1. 用户认证系统

- **注册登录**: 支持用户名 + 密码注册和登录
- **JWT 认证**: 使用 JWT Token 进行身份验证，有效期 24 小时
- **密码加密**: 使用 BCrypt 对密码进行加密存储
- **设备管理**: 支持查看和管理登录设备
- **登录历史**: 记录用户的登录历史

### 2. 矿物识别功能

- **图片上传**: 支持 JPG/PNG 格式图片上传，最大 10MB
- **AI 识别**: 集成 YOLO 模型进行矿物识别
- **结果展示**: 显示矿物名称、置信度、边界框
- **详细信息**: 提供矿物的化学式、硬度、产地等详细信息
- **历史记录**: 保存所有识别记录供用户查看

### 3. 智能问答系统

- **会话管理**: 创建、查看、删除聊天会话
- **流式响应**: 使用 SSE 技术实现打字机效果
- **上下文理解**: 支持基于矿物识别结果的问答
- **消息持久化**: 保存所有聊天历史

### 4. 数据统计与成就

- **使用统计**: 统计识别次数、问答次数、活跃天数等
- **成就系统**: 9 种不同类型的成就，激励用户使用
- **矿物频率**: 统计用户识别的矿物种类和频率
- **可视化图表**: 使用图表展示统计数据

### 5. 用户个性化

- **个人资料**: 支持修改昵称、头像、邮箱
- **偏好设置**: 支持主题切换、语言设置
- **通知管理**: 控制邮件通知和系统通知开关

### 6. 历史记录管理

- **识别历史**: 查看所有识别记录，支持筛选和搜索
- **聊天历史**: 查看所有聊天会话
- **数据导出**: 支持导出个人数据
- **批量删除**: 支持删除历史记录

---

## 部署说明

### 后端部署

1. **打包项目**
   ```bash
   cd backend
   mvn clean package
   ```

2. **配置生产环境**
   编辑 `application.yml`，配置生产数据库和 Ollama 地址

3. **运行服务**
   ```bash
   java -jar target/mineral-system-1.0.0.jar
   ```

4. **使用 Docker 部署**（可选）
   ```bash
   docker build -t mineral-system .
   docker run -d -p 8080:8080 mineral-system
   ```

### 前端部署

1. **构建项目**
   ```bash
   cd frontend
   npm run build
   ```

2. **部署到 Nginx**
   将 `dist/` 目录部署到 Nginx 服务器

3. **Nginx 配置示例**
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;
       
       location / {
           root /path/to/dist;
           index index.html;
           try_files $uri $uri/ /index.html;
       }
       
       location /api/ {
           proxy_pass http://localhost:8080/api/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

---

## 错误码说明

### 通用错误
- `0`: 成功
- `1000`: 参数错误
- `1001`: 请求格式错误
- `1002`: 数据不存在
- `1003`: 数据已存在

### 认证错误
- `2000`: 未登录
- `2001`: Token 无效
- `2002`: Token 已过期
- `2003`: 用户名或密码错误
- `2004`: 用户已被禁用

### 用户错误
- `3000`: 用户名已存在
- `3001`: 邮箱已存在
- `3002`: 原密码错误
- `3003`: 新密码格式错误

### 矿物识别错误
- `4000`: 图片格式不支持
- `4001`: 图片大小超限
- `4002`: 识别失败
- `4003`: 识别记录不存在

### 聊天错误
- `5000`: 会话不存在
- `5001`: 会话已删除
- `5002`: 消息内容为空
- `5003`: AI 服务不可用

---

## 常见问题

### 1. 启动报错：找不到或无法加载主类
确保使用 JDK 17，检查 `JAVA_HOME` 环境变量。

### 2. 数据库连接失败
检查 MySQL 服务是否启动，用户名密码是否正确。

### 3. 文件上传失败
确保 `uploads/` 目录存在且有写权限。

### 4. JWT Token 无效
检查请求头格式是否正确：`Authorization: Bearer <token>`（注意 Bearer 后有空格）

### 5. Ollama 连接失败
确保 Ollama 服务已启动，并配置正确的 API 地址。

---

## 开发团队

- **后端开发**: Spring Boot 团队
- **前端开发**: Vue 3 团队
- **AI 集成**: YOLO + Ollama

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
