# 快速部署指南

## 一、环境准备

### 1. 安装 JDK 17

**Windows:**
```bash
# 下载 JDK 17
# 访问：https://www.oracle.com/java/technologies/downloads/#jdk17-windows

# 安装后配置环境变量
JAVA_HOME=C:\Program Files\Java\jdk-17
Path=%JAVA_HOME%\bin
```

**验证安装:**
```bash
java -version
```

### 2. 安装 MySQL 8.0

**Windows:**
```bash
# 下载 MySQL Installer
# 访问：https://dev.mysql.com/downloads/installer/

# 安装后配置
# 默认端口：3306
# 用户名：root
# 密码：自行设置
```

### 3. 安装 Maven（可选，已有 Maven Wrapper）

```bash
# 下载 Maven
# 访问：https://maven.apache.org/download.cgi

# 配置环境变量
MAVEN_HOME=C:\apache-maven-3.8.6
Path=%MAVEN_HOME%\bin
```

## 二、数据库初始化

### 1. 登录 MySQL

```bash
mysql -u root -p
```

### 2. 执行初始化脚本

```sql
-- 方式一：在 MySQL 命令行中
source d:/OneDrive/Desktop/毕业设计/mineral-system/backend/src/main/resources/db/init.sql

-- 方式二：使用命令行
mysql -u root -p < d:/OneDrive/Desktop/毕业设计/mineral-system/backend/src/main/resources/db/init.sql
```

### 3. 验证数据库

```sql
USE mineral_system;
SHOW TABLES;
-- 应该看到：users, detections, detection_results, minerals, chat_sessions, chat_messages
```

## 三、项目配置

### 1. 修改数据库配置

编辑 `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mineral_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root          # 修改为你的 MySQL 用户名
    password: your_password # 修改为你的 MySQL 密码
```

### 2. 创建上传目录

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
mkdir uploads
```

## 四、编译项目

### 方式一：使用 Maven 命令

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
mvn clean install
```

### 方式二：使用批处理脚本

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
build.bat
```

## 五、启动项目

### 方式一：使用 Maven

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
mvn spring-boot:run
```

### 方式二：使用批处理脚本

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
start.bat
```

### 方式三：运行 JAR 包

```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
java -jar target/mineral-system-1.0.0.jar
```

## 六、验证服务

### 1. 检查服务状态

访问：http://localhost:8080/api

如果看到 404 或 401，说明服务正常启动（需要认证）。

### 2. 测试接口

**注册新用户:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "email": "admin@example.com"
  }'
```

**用户登录:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

响应示例:
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

**获取用户信息:**
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 七、常见问题

### 1. 端口被占用

**错误:** `Port 8080 was already in use`

**解决:**
```yaml
# 修改 application.yml
server:
  port: 8081  # 改为其他端口
```

### 2. 数据库连接失败

**错误:** `Communications link failure`

**检查:**
- MySQL 服务是否启动
- 用户名密码是否正确
- 数据库 mineral_system 是否存在

### 3. JWT Token 无效

**错误:** `Token 无效或已过期`

**检查:**
- 请求头格式：`Authorization: Bearer <token>`（注意空格）
- Token 是否过期（有效期 24 小时）

### 4. 文件上传失败

**错误:** `文件上传失败`

**检查:**
- `uploads/` 目录是否存在
- 目录是否有写权限
- 文件大小是否超过 10MB

## 八、生产环境部署

### 1. 打包项目

```bash
mvn clean package -DskipTests
```

### 2. 修改生产配置

创建 `application-prod.yml`:
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://生产服务器 IP:3306/mineral_system?useUnicode=true&characterEncoding=utf8
    username: prod_user
    password: prod_password

jwt:
  secret: 生产环境使用更复杂的密钥
  expiration: 604800000  # 7 天
```

### 3. 启动服务

```bash
java -jar -Dspring.profiles.active=prod mineral-system-1.0.0.jar
```

### 4. 配置 Nginx（可选）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /uploads/ {
        alias /path/to/backend/uploads/;
    }
}
```

## 九、监控和维护

### 1. 查看日志

```bash
# 日志文件位置
tail -f logs/mineral-system.log
```

### 2. 数据库备份

```bash
mysqldump -u root -p mineral_system > backup.sql
```

### 3. 恢复数据库

```bash
mysql -u root -p mineral_system < backup.sql
```

## 十、性能优化建议

### 1. JVM 参数优化

```bash
java -Xms512m -Xmx2g -jar mineral-system-1.0.0.jar
```

### 2. 数据库连接池

在 `application.yml` 中添加:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. 启用 Redis 缓存（可选）

添加 Redis 依赖并配置缓存策略。

---

**部署完成！** 🎉

如有问题，请查看 README.md 或联系开发团队。
