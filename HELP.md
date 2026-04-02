# 项目文件结构

```
mineralDO-system/
│
├── backend/                                    # 后端项目根目录
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mineralDO/              # Java 源代码
│   │   │   │   │
│   │   │   │   ├── MineralSystemApplication.java    # 🚀 主启动类
│   │   │   │   │
│   │   │   │   ├── common/                         # 📦 通用模块
│   │   │   │   │   ├── ApiResponse.java           # 统一响应格式
│   │   │   │   │   ├── PageResult.java            # 分页结果
│   │   │   │   │   ├── PageQuery.java             # 分页查询
│   │   │   │   │   ├── ErrorCode.java             # 错误码定义
│   │   │   │   │   ├── BusinessException.java     # 业务异常
│   │   │   │   │   └── GlobalExceptionHandler.java # 全局异常处理
│   │   │   │   │
│   │   │   │   ├── config/                         # ⚙️ 配置类
│   │   │   │   │   ├── CorsConfig.java            # CORS 跨域配置
│   │   │   │   │   ├── JwtUtil.java               # JWT 工具类
│   │   │   │   │   ├── JwtAuthenticationFilter.java # JWT 认证过滤器
│   │   │   │   │   ├── MybatisPlusConfig.java     # MyBatis Plus 配置
│   │   │   │   │   ├── SecurityConfig.java        # Spring Security 配置
│   │   │   │   │   ├── SecurityFilterConfig.java  # 安全过滤器链
│   │   │   │   │   └── WebConfig.java             # Web 配置
│   │   │   │   │
│   │   │   │   ├── controller/                     # 🎮 控制器层
│   │   │   │   │   ├── AuthController.java        # 认证接口 (3 个)
│   │   │   │   │   ├── UserController.java        # 用户接口 (3 个)
│   │   │   │   │   ├── MineralController.java     # 矿物接口 (4 个)
│   │   │   │   │   ├── ChatController.java        # 聊天接口 (5 个)
│   │   │   │   │   ├── HistoryController.java     # 历史接口 (3 个)
│   │   │   │   │   └── StatsController.java       # 统计接口 (2 个)
│   │   │   │   │
│   │   │   │   ├── dto/                            # 📋 数据传输对象
│   │   │   │   │   # 请求 DTO (5 个)
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── UpdateProfileRequest.java
│   │   │   │   │   ├── UpdatePasswordRequest.java
│   │   │   │   │   ├── CreateSessionRequest.java
│   │   │   │   │   ├── SendMessageRequest.java
│   │   │   │   │   ├── DetectionHistoryQuery.java
│   │   │   │   │   │
│   │   │   │   │   # 响应 DTO (13 个)
│   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   ├── UserProfileResponse.java
│   │   │   │   │   ├── MineralInfoResponse.java
│   │   │   │   │   ├── DetectionResponse.java
│   │   │   │   │   ├── DetectionResultResponse.java
│   │   │   │   │   ├── MineralCategoryResponse.java
│   │   │   │   │   ├── ChatSessionResponse.java
│   │   │   │   │   ├── ChatMessageResponse.java
│   │   │   │   │   ├── DetectionHistoryResponse.java
│   │   │   │   │   ├── StatsOverviewResponse.java
│   │   │   │   │   ├── MineralFrequencyResponse.java
│   │   │   │   │   └── SSEToken.java
│   │   │   │   │
│   │   │   │   ├── entity/                         # 💾 实体类
│   │   │   │   │   ├── User.java                  # 用户实体
│   │   │   │   │   ├── Detection.java             # 识别记录实体
│   │   │   │   │   ├── DetectionResult.java       # 识别结果实体
│   │   │   │   │   ├── Mineral.java               # 矿物信息实体
│   │   │   │   │   ├── ChatSession.java           # 聊天会话实体
│   │   │   │   │   └── ChatMessage.java           # 聊天消息实体
│   │   │   │   │
│   │   │   │   ├── mapper/                         # 🔍 Mapper 接口
│   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   ├── DetectionMapper.java
│   │   │   │   │   ├── DetectionResultMapper.java
│   │   │   │   │   ├── MineralMapper.java
│   │   │   │   │   ├── ChatSessionMapper.java
│   │   │   │   │   └── ChatMessageMapper.java
│   │   │   │   │
│   │   │   │   └── service/                        # 🔧 服务层
│   │   │   │       ├── AuthService.java           # 认证服务
│   │   │   │       ├── UserService.java           # 用户服务
│   │   │   │       ├── MineralService.java        # 矿物识别服务
│   │   │   │       ├── ChatService.java           # 聊天服务
│   │   │   │       ├── HistoryService.java        # 历史记录服务
│   │   │   │       └── StatsService.java          # 统计服务
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml                # 📝 应用配置文件
│   │   │       └── db/
│   │   │           └── init.sql                   # 📊 数据库初始化脚本
│   │   │
│   │   └── test/                                  # 🧪 测试代码（待添加）
│   │
│   ├── uploads/                                   # 📁 文件上传目录
│   │
│   ├── target/                                    # 🎯 编译输出目录
│   │
│   ├── .gitignore                                 # Git 忽略文件
│   ├── pom.xml                                    # Maven 配置
│   │
│   ├── start.bat                                  # 🚀 启动脚本
│   ├── build.bat                                  # 📦 打包脚本
│   │
│   ├── README.md                                  # 📖 项目说明文档
│   ├── QUICKSTART.md                              # ⚡ 快速启动指南
│   ├── DEPLOYMENT.md                              # 🚀 部署指南
│   ├── API_SPECIFICATION.md                       # 📋 API 规格说明书
│   ├── PROJECT_SUMMARY.md                         # 📊 项目总结
│   ├── postman_collection.json                    # 🧪 Postman 测试集合
│   │
│   └── HELP.md                                    # ❓ 帮助文档（本文件）
│
├── frontend/                                      # 前端项目（Vue 3）
│   ├── src/
│   │   ├── api/                                   # API 接口
│   │   ├── components/                            # 组件
│   │   ├── views/                                 # 页面
│   │   ├── stores/                                # 状态管理
│   │   ├── router/                                # 路由
│   │   └── utils/                                 # 工具函数
│   └── package.json                               # npm 配置
│
└── HELP.md                                        # 总帮助文档
```

## 文件统计

### Java 源代码
- **实体类**: 6 个
- **DTO 类**: 18 个
- **Controller**: 6 个
- **Service**: 6 个
- **Mapper**: 6 个
- **Config**: 7 个
- **Common**: 6 个
- **总计**: 55 个 Java 文件

### 配置文件
- **application.yml**: 主配置文件
- **pom.xml**: Maven 依赖配置
- **init.sql**: 数据库初始化脚本

### 文档文件
- **README.md**: 项目说明
- **QUICKSTART.md**: 快速启动
- **DEPLOYMENT.md**: 部署指南
- **API_SPECIFICATION.md**: API 文档
- **PROJECT_SUMMARY.md**: 项目总结
- **postman_collection.json**: Postman 测试

### 脚本文件
- **start.bat**: Windows 启动脚本
- **build.bat**: Windows 打包脚本

## 核心模块依赖关系

```
┌─────────────────────────────────────────────────────┐
│                  Controller 层                        │
│  (Auth/User/Mineral/Chat/History/StatsController)   │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│                   Service 层                          │
│   (AuthService/UserService/MineralService/...)      │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│                   Mapper 层                           │
│    (UserMapper/DetectionMapper/MineralMapper/...)   │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│                  Database                           │
│         (MySQL: users/detectionDOS/minerals/...)      │
└─────────────────────────────────────────────────────┘
```

## 配置层级

```
application.yml
    ├── server (端口、上下文路径)
    ├── spring (数据源、文件上传、Jackson)
    ├── mybatis-plus (配置、全局设置)
    ├── jwt (密钥、过期时间)
    └── upload (上传路径、大小限制)
```

## API 接口分布

```
/api
├── /auth          # 认证模块
│   ├── POST /register
│   ├── POST /login
│   └── POST /logout
│
├── /userDO          # 用户模块
│   ├── GET  /profile
│   ├── PUT  /profile
│   └── PUT  /password
│
├── /mineralDO       # 矿物识别模块
│   ├── POST /detect
│   ├── GET  /detect/{id}
│   ├── GET  /categories
│   └── GET  /info/{name}
│
├── /chat          # 聊天会话模块
│   ├── POST /session
│   ├── GET  /sessions
│   ├── GET  /session/{id}/messages
│   ├── POST /session/{id}/send
│   └── DELETE /session/{id}
│
├── /history       # 历史记录模块
│   ├── GET  /detectionDOS
│   ├── DELETE /detectionDOS/{id}
│   └── GET  /chats
│
└── /stats         # 统计模块
    ├── GET  /overview
    └── GET  /mineralDO-frequency
```

## 数据库表关系

```
users (用户表)
    ├── 1:N → detectionDOS (识别记录)
    └── 1:N → chat_sessions (聊天会话)

detectionDOS (识别记录表)
    ├── N:1 → users (用户表)
    └── 1:N → detection_results (识别结果)

detection_results (识别结果表)
    └── N:1 → detectionDOS (识别记录)

minerals (矿物信息表)
    └── 被 detection_results 引用

chat_sessions (聊天会话表)
    ├── N:1 → users (用户表)
    ├── 1:1 → detectionDOS (识别记录，可选)
    └── 1:N → chat_messages (聊天消息)

chat_messages (聊天消息表)
    └── N:1 → chat_sessions (会话表)
```

---

**提示**: 本文件提供了项目的完整文件结构视图，帮助快速了解项目组织。
