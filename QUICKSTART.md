# 快速启动指南

## 一分钟快速启动

### 步骤 1: 初始化数据库

```bash
# 打开 MySQL 命令行
mysql -u root -p

# 执行初始化脚本（替换为实际路径）
source d:/OneDrive/Desktop/毕业设计/mineral-system/backend/src/main/resources/db/init.sql

# 退出
exit
```

### 步骤 2: 修改配置

编辑 `application.yml`，修改数据库密码：

```yaml
spring:
  datasource:
    username: root
    password: your_password  # 改为你的 MySQL 密码
```

### 步骤 3: 启动项目

**方式一：使用启动脚本（推荐）**
```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
start.bat
```

**方式二：使用 Maven 命令**
```bash
cd d:/OneDrive/Desktop/毕业设计/mineral-system/backend
mvn spring-boot:run
```

### 步骤 4: 测试接口

**打开浏览器访问：**
```
http://localhost:8080/api
```

**使用 Postman 测试：**
1. 导入 `postman_collection.json` 到 Postman
2. 执行 "用户注册" 接口
3. 执行 "用户登录" 接口（自动保存 Token）
4. 测试其他接口

## 测试账号

数据库初始化脚本已创建测试账号：

```
用户名：testuser
密码：123456
邮箱：test@example.com
```

## 快速测试示例

### 1. 注册新用户

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"email\":\"admin@example.com\"}"
```

### 2. 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

保存返回的 token。

### 3. 获取用户信息

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. 获取矿物信息

```bash
curl -X GET http://localhost:8080/api/mineral/info/石英
```

## 项目结构概览

```
backend/
├── src/main/java/com/mineral/
│   ├── common/           # 通用类（响应、异常、分页）
│   ├── config/           # 配置类（JWT、Security、CORS）
│   ├── controller/       # 控制器（6 个）
│   ├── dto/              # 数据传输对象（20 个）
│   ├── entity/           # 实体类（6 个）
│   ├── mapper/           # Mapper 接口（6 个）
│   └── service/          # 服务类（6 个）
├── src/main/resources/
│   ├── application.yml   # 配置文件
│   └── db/init.sql       # 数据库初始化
├── README.md             # 详细说明
├── DEPLOYMENT.md         # 部署指南
└── API_SPECIFICATION.md  # API 文档
```

## 接口分类

| 分类 | 路径前缀 | 接口数 |
|------|---------|--------|
| 认证 | /auth | 3 |
| 用户 | /user | 3 |
| 矿物 | /mineral | 4 |
| 聊天 | /chat | 5 |
| 历史 | /history | 3 |
| 统计 | /stats | 2 |

## 常用命令

### 开发
```bash
# 启动项目
mvn spring-boot:run

# 清理编译
mvn clean

# 编译项目
mvn compile
```

### 打包
```bash
# 打包（跳过测试）
mvn clean package -DskipTests

# 运行 JAR
java -jar target/mineral-system-1.0.0.jar
```

### 数据库
```bash
# 备份数据库
mysqldump -u root -p mineral_system > backup.sql

# 恢复数据库
mysql -u root -p mineral_system < backup.sql
```

## 故障排查

### 问题 1: 端口被占用
```
错误：Port 8080 was already in use
解决：修改 application.yml 中的 server.port
```

### 问题 2: 数据库连接失败
```
错误：Communications link failure
解决：检查 MySQL 服务是否启动，密码是否正确
```

### 问题 3: 编译失败
```
错误：找不到或无法加载主类
解决：确保使用 JDK 17，检查 JAVA_HOME 环境变量
```

## 下一步

1. ✅ 完成数据库初始化
2. ✅ 启动后端服务
3. ✅ 测试 API 接口
4. 📝 阅读 API_SPECIFICATION.md 了解详细接口
5. 📝 阅读 DEPLOYMENT.md 了解部署流程
6. 📝 阅读 PROJECT_SUMMARY.md 了解项目总结

---

**提示**: 更多详细信息请查看对应文档：
- 详细说明：README.md
- 部署指南：DEPLOYMENT.md  
- API 文档：API_SPECIFICATION.md
- 项目总结：PROJECT_SUMMARY.md
