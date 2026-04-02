# 快速开始指南

## 前提条件

在开始之前，请确保已安装以下内容：

### 1. JDK 17（必需）
- 下载：https://www.oracle.com/java/technologies/downloads/#java17
- 安装后设置环境变量（**重要！**）：

```powershell
# 以管理员身份运行 PowerShell
setx JAVA_HOME "C:\Program Files\Java\jdk-17" /M
setx PATH "%JAVA_HOME%\bin;%PATH%" /M
```

验证安装：
```cmd
java -version
# 应该显示 java version "17.x.x"
```

### 2. Ollama（用于 AI 聊天）
1. 下载并安装：https://ollama.ai
2. 拉取模型：
   ```cmd
   ollama pull qwen2.5:7b
   ```
3. 启动服务：
   ```cmd
   ollama serve
   ```

### 3. MySQL 数据库
- 确保 MySQL 正在运行
- 数据库配置在 `application.yml`

## 快速启动

### 方法 1：使用提供的脚本（推荐）

1. **构建项目**
   ```cmd
   build-with-jdk17.bat
   ```

2. **运行项目**
   ```cmd
   run-with-jdk17.bat
   ```

### 方法 2：手动启动

1. **设置 JAVA_HOME（如果未设置）**
   ```cmd
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

2. **构建项目**
   ```cmd
   mvn clean package -DskipTests
   ```

3. **运行项目**
   ```cmd
   java -jar target\mineralDO-system-1.0.0.jar
   ```

## 验证运行

应用启动后，会显示：
```
Started MineralSystemApplication in X.XXX seconds
```

访问 http://localhost:8080/api/auth/register 测试 API

## 测试 AI 聊天功能

1. **注册用户**
   ```bash
   POST http://localhost:8080/api/auth/register
   Content-Type: application/json
   
   {
     "username": "testuser",
     "password": "123456",
     "email": "test@example.com"
   }
   ```

2. **登录获取 Token**
   ```bash
   POST http://localhost:8080/api/auth/login
   Content-Type: application/json
   
   {
     "username": "testuser",
     "password": "123456"
   }
   ```

3. **创建聊天会话**
   ```bash
   POST http://localhost:8080/api/chat/session
   Authorization: Bearer <your-token>
   Content-Type: application/json
   
   {
     "title": "测试会话"
   }
   ```

4. **发送消息（AI 回复）**
   ```bash
   POST http://localhost:8080/api/chat/session/{sessionId}/send
   Authorization: Bearer <your-token>
   Content-Type: application/json
   
   {
     "content": "你好，请介绍一下石英矿"
   }
   ```

   响应是 SSE 流式返回，你会看到 AI 逐字输出回答。

## 常见问题

### Q: "JAVA_HOME environment variable is not set"
**A**: 按照上面的步骤设置 JAVA_HOME 环境变量，然后**重启终端**

### Q: Maven 下载依赖很慢
**A**: 已配置阿里云镜像，首次构建会较慢，请耐心等待

### Q: Ollama 连接失败
**A**: 
1. 确保 Ollama 正在运行：`ollama serve`
2. 确保已下载模型：`ollama list` 应该看到 `qwen2.5:7b`

### Q: 数据库连接失败
**A**: 检查 `application.yml` 中的数据库配置，确保 MySQL 正在运行

## 下一步

- 查看 [UPGRADE_GUIDE.md](UPGRADE_GUIDE.md) 了解详细的升级说明
- 查看 [API_SPECIFICATION.md](API_SPECIFICATION.md) 了解完整的 API 文档
- 查看 [README.md](README.md) 了解项目概述

## 技术支持

如有问题，请查看：
- Spring AI 文档：https://docs.spring.io/spring-ai/reference/
- Ollama 文档：https://ollama.ai/
