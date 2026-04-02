# Spring Boot 3.2 + Spring AI 升级指南

## 升级概述

本项目已从 Spring Boot 2.7.18 升级到 Spring Boot 3.2.4，并集成了 Spring AI 来调用 Ollama 接口。

## 主要变更

### 1. Java 版本升级
- **升级前**: Java 8
- **升级后**: Java 17 或更高版本
- **原因**: Spring Boot 3.x 最低要求 Java 17

### 2. Spring Boot 版本
- **升级前**: 2.7.18
- **升级后**: 3.2.4

### 3. Spring AI 集成
- 添加 Spring AI Ollama Starter 依赖 (版本：1.0.0-M6)
- 配置 Ollama 连接参数
- 实现流式聊天功能

### 4. Jakarta EE 迁移
- 所有 `javax.*` 包名已更改为 `jakarta.*`
- 影响的包：
  - `javax.servlet` → `jakarta.servlet`
  - `javax.validation` → `jakarta.validation`

### 5. Spring Security 6.x 更新
- `EnableGlobalMethodSecurity` → `EnableMethodSecurity`
- 安全配置链使用新的 Lambda DSL 语法
- `antMatchers()` → `requestMatchers()`

### 6. 新增依赖
```xml
<!-- Spring AI Ollama -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<!-- Spring Boot WebFlux (用于响应式流) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## 环境要求

### 必需
- **JDK**: 17 或 21
- **Maven**: 3.6+
- **Ollama**: 已安装并运行在 http://localhost:11434

### 安装 JDK 17

#### Windows
1. 下载 JDK 17: https://www.oracle.com/java/technologies/downloads/#java17
2. 安装后设置环境变量：
   ```powershell
   # 设置 JAVA_HOME
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   
   # 更新 PATH
   setx PATH "%JAVA_HOME%\bin;%PATH%"
   ```

3. 验证安装：
   ```powershell
   java -version
   # 应该显示 Java 17.x.x
   ```

### 安装 Ollama

1. 访问 https://ollama.ai 下载并安装 Ollama
2. 拉取模型（推荐使用 qwen2.5:7b）：
   ```bash
   ollama pull qwen2.5:7b
   ```
3. 启动 Ollama：
   ```bash
   ollama serve
   ```

## 配置说明

### application.yml 配置

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b  # 使用的模型名称
        options:
          temperature: 0.7  # 温度参数，控制随机性
          num-predict: 2048  # 最大预测 token 数
```

### 可配置的 Ollama 参数

- `base-url`: Ollama 服务地址
- `model`: 使用的模型（如：qwen2.5:7b, llama2, mistral 等）
- `temperature`: 温度参数 (0.0-1.0)，越高越随机
- `num-predict`: 最大生成 token 数

## 使用示例

### 调用 Ollama 聊天

ChatService 已经集成了 Ollama 调用：

```java
@Service
public class ChatService {
    private final ChatClient chatClient;
    
    // 流式调用
    public Flux<String> chatWithOllama(String sessionId, String content, String mineralContext) {
        String systemPrompt = buildSystemPrompt(mineralContext);
        String userMessage = buildUserMessage(content, mineralContext);
        
        return chatClient.prompt()
            .system(systemPrompt)
            .userDO(userMessage)
            .stream()
            .content();
    }
}
```

### SSE 流式响应

ChatController 使用 SSE (Server-Sent Events) 实现流式响应：

```java
@PostMapping(value = "/session/{sessionId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sendMessage(
        @PathVariable String sessionId,
        @Valid @RequestBody SendMessageRequest request,
        HttpServletRequest httpRequest) {
    
    SseEmitter emitter = new SseEmitter(0L);
    
    // 订阅 Flux 并流式发送到客户端
    Flux<String> response = chatService.chatWithOllama(...);
    response.subscribe(token -> {
        emitter.send(SseEmitter.event().data("{\"token\": \"" + token + "\"}"));
    });
    
    return emitter;
}
```

## API 端点

### 发送消息（流式）
```
POST /api/chat/session/{sessionId}/send
Content-Type: application/json

{
  "content": "你好，请介绍一下石英矿",
  "mineralContext": "石英 - 硬度 7，主要成分 SiO2"
}
```

响应是 SSE 流，格式：
```
event: message
data: {"token": "你"}

event: message
data: {"token": "好"}

event: done
data: {"done": true, "messageId": "assistant_1234567890"}
```

## 构建和运行

### 前提条件
1. 安装 JDK 17+
2. 安装并运行 Ollama
3. 拉取所需的 Ollama 模型

### 构建
```bash
mvn clean package -DskipTests
```

### 运行
```bash
java -jar target/mineralDO-system-1.0.0.jar
```

或者使用 Maven：
```bash
mvn spring-boot:run
```

## 自定义系统提示词

在 ChatService 中的 `buildSystemPrompt()` 方法可以自定义 AI 助手的行为：

```java
private String buildSystemPrompt(String mineralContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是一个专业的矿物识别助手...\n");
    // 添加你的自定义提示词
    return sb.toString();
}
```

## 故障排查

### 问题 1：编译错误 "无效的标记：--release"
**错误信息**: 
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project mineralDO-system: Fatal error compiling: 无效的标记：--release
```

**原因**: Maven 使用的 JDK 版本低于 17  
**解决方案**:

1. **安装 JDK 17**（如果尚未安装）
   - 下载地址：https://www.oracle.com/java/technologies/downloads/#java17
   - 选择 Windows x64 Installer

2. **设置 JAVA_HOME 环境变量**
   ```powershell
   # 以管理员身份运行 PowerShell
   setx JAVA_HOME "C:\Program Files\Java\jdk-17" /M
   setx PATH "%JAVA_HOME%\bin;%PATH%" /M
   ```
   
3. **重启终端**并验证：
   ```powershell
   echo %JAVA_HOME%
   java -version
   mvn -version
   ```
   确保都显示 Java 17

4. **或者使用提供的脚本**
   ```cmd
   build-with-jdk17.bat
   ```

### 问题 2：运行时错误 "Invalid value type for attribute 'factoryBeanObjectType'"
**错误信息**:
```
java.lang.IllegalArgumentException: Invalid value type for attribute 'factoryBeanObjectType': java.lang.String
```

**原因**: MyBatis Plus 版本与 Spring Boot 3.2 不兼容  
**解决方案**:
- 已修复！MyBatis Plus 已升级到 3.5.6
- 如果仍有问题，确保使用的是最新依赖：
  ```xml
  <mybatis-plus.version>3.5.6</mybatis-plus.version>
  ```

### 问题 3：无法连接 Ollama
**原因**: Ollama 服务未启动或地址配置错误  
**解决方案**: 
1. 确保 Ollama 正在运行：`ollama serve`
2. 检查 `application.yml` 中的 `base-url` 配置
3. 测试连接：`curl http://localhost:11434/api/version`

### 问题 4：模型不存在
**原因**: 未下载指定的 Ollama 模型  
**解决方案**: `ollama pull qwen2.5:7b`

### 问题 5：Maven 构建慢或下载依赖失败
**解决方案**:
1. 使用阿里云 Maven 镜像（已配置）
2. 清理本地仓库后重新构建：
   ```cmd
   mvn clean -U
   ```

## 技术栈

- Spring Boot 3.2.4
- Spring AI 1.0.0-M6
- Spring Security 6.2.3
- MyBatis Plus 3.5.5
- JJWT 0.12.5
- Lombok
- Hutool 5.8.24

## 注意事项

1. **Java 版本**: 必须使用 Java 17 或更高版本
2. **Ollama 服务**: 需要本地运行 Ollama 服务
3. **模型选择**: 可以根据需求更换 Ollama 模型
4. **SSE 支持**: 前端需要支持 EventSource API

## 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Ollama 官方文档](https://ollama.ai/)
- [Spring Boot 3.2 迁移指南](https://spring.io/blog/2023/11/24/spring-boot-3-2-0-available-now)
- [Jakarta EE 迁移指南](https://jakarta.ee/blogs/jakarta-ee-10-takes-you-to-new-heights/)
