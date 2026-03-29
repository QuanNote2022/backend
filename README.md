# 矿物识别系统后端

基于 Spring Boot 3.2 的矿物识别系统后端服务。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **安全**: Spring Security + JWT
- **ORM**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0+
- **工具**: Lombok, Hutool
- **构建工具**: Maven

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/init.sql
```

### 3. 配置修改

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mineral_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 4. 编译项目

```bash
mvn clean install
```

### 5. 运行项目

```bash
# 方式一：使用 Maven
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/mineral-system-1.0.0.jar
```

### 6. 访问服务

服务启动后访问：http://localhost:8080/api

## API 接口文档

### 认证模块
- `POST /auth/register` - 用户注册
- `POST /auth/login` - 用户登录
- `POST /auth/logout` - 用户登出

### 用户模块
- `GET /user/profile` - 获取用户信息
- `PUT /user/profile` - 更新用户信息
- `PUT /user/password` - 修改密码

### 矿物识别模块
- `POST /mineral/detect` - 矿物识别（上传图片）
- `GET /mineral/detect/{detectId}` - 获取识别详情
- `GET /mineral/categories` - 获取矿物分类
- `GET /mineral/info/{mineralName}` - 获取矿物信息

### 聊天会话模块
- `POST /chat/session` - 创建聊天会话
- `GET /chat/sessions` - 获取会话列表
- `GET /chat/session/{sessionId}/messages` - 获取会话消息
- `POST /chat/session/{sessionId}/send` - 发送消息（SSE）
- `DELETE /chat/session/{sessionId}` - 删除会话

### 历史记录模块
- `GET /history/detections` - 获取识别历史
- `DELETE /history/detections/{detectId}` - 删除识别记录
- `GET /history/chats` - 获取聊天历史

### 统计模块
- `GET /stats/overview` - 获取统计概览
- `GET /stats/mineral-frequency` - 获取矿物识别频率

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/mineral/
│   │   │   ├── MineralSystemApplication.java  # 启动类
│   │   │   ├── common/                        # 通用类
│   │   │   │   ├── ApiResponse.java           # 统一响应
│   │   │   │   ├── PageResult.java            # 分页结果
│   │   │   │   ├── PageQuery.java             # 分页查询
│   │   │   │   ├── ErrorCode.java             # 错误码
│   │   │   │   ├── BusinessException.java     # 业务异常
│   │   │   │   └── GlobalExceptionHandler.java # 全局异常处理
│   │   │   ├── config/                        # 配置类
│   │   │   │   ├── MybatisPlusConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── SecurityFilterConfig.java
│   │   │   ├── controller/                    # 控制器
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── MineralController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── HistoryController.java
│   │   │   │   └── StatsController.java
│   │   │   ├── dto/                           # 数据传输对象
│   │   │   ├── entity/                        # 实体类
│   │   │   ├── mapper/                        # Mapper 接口
│   │   │   └── service/                       # 服务层
│   │   └── resources/
│   │       ├── application.yml                # 配置文件
│   │       └── db/
│   │           └── init.sql                   # 数据库初始化脚本
│   └── test/
└── pom.xml
```

## 认证说明

### JWT Token 使用

1. 用户登录后获取 Token
2. 在请求头中携带 Token：`Authorization: Bearer <token>`
3. Token 有效期：24 小时

### 无需认证的接口

- `/auth/**` - 认证相关接口
- `/mineral/info/**` - 矿物信息查询
- `/mineral/categories` - 矿物分类列表

## 文件上传

### 配置说明

- 上传路径：`./uploads/`（相对于项目根目录）
- 最大文件大小：10MB
- 支持格式：图片格式（JPG、PNG 等）

### 文件存储结构

```
uploads/
└── 2024/
    └── 01/
        └── 01/
            └── uuid_filename.jpg
```

## 错误码说明

详见 `API_SPECIFICATION.md` 文档第 8 章节。

## 开发说明

### 添加新的矿物数据

编辑 `src/main/resources/db/init.sql`，在 `minerals` 表中添加数据。

### 集成真实的 AI 模型

当前使用模拟数据进行矿物识别。要集成真实的 YOLO 模型：

1. 添加 Python 服务进行模型推理
2. 或使用 Java DL 库（如 DJL、DeepJavaLibrary）
3. 修改 `MineralService.simulateDetection()` 方法

### 集成大语言模型

当前聊天功能使用模拟响应。要集成真实的 LLM：

1. 调用 ChatGLM、Qwen 等 API
2. 修改 `ChatController.sendMessage()` 方法
3. 实现真实的 SSE 流式响应

## 常见问题

### 1. 启动报错：找不到或无法加载主类

确保使用 JDK 17，检查 `JAVA_HOME` 环境变量。

### 2. 数据库连接失败

检查 MySQL 服务是否启动，用户名密码是否正确。

### 3. 文件上传失败

确保 `uploads/` 目录存在且有写权限。

### 4. JWT Token 无效

检查请求头格式是否正确：`Authorization: Bearer <token>`（注意 Bearer 后有空格）

## 许可证

本项目的许可证信息。

## 联系方式

- 作者：XXX
- 邮箱：xxx@example.com
