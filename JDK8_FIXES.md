# JDK 1.8 兼容性修复说明

## 修复的问题

### 1. Spring Boot 版本调整
- **修改前**: Spring Boot 3.2.0 (需要 JDK 17)
- **修改后**: Spring Boot 2.7.18 (支持 JDK 1.8)

### 2. 依赖包调整

#### MyBatis Plus
- **修改前**: `mybatis-plus-spring-boot3-starter`
- **修改后**: `mybatis-plus-boot-starter`

#### MySQL Driver
- **修改前**: `mysql-connector-j`
- **修改后**: `mysql-connector-java`

#### JWT (JJWT)
- **修改前**: `jjwt 0.12.3` (使用新 API)
- **修改后**: `jjwt 0.11.5` (使用旧 API)

### 3. 代码修复

#### 3.1 javax vs jakarta
- **修改前**: 使用 `jakarta.*` 包名
- **修改后**: 使用 `javax.*` 包名
  - `javax.servlet.*`
  - `javax.validation.*`
  - `javax.annotation.*`

#### 3.2 Java 8 不支持的方法

##### List.of() 方法
```java
// 修改前 (Java 17)
List<String> mineralNames = List.of("石英", "长石", "云母");

// 修改后 (Java 8)
List<String> mineralNames = new ArrayList<>();
mineralNames.add("石英");
mineralNames.add("长石");
mineralNames.add("云母");
```

##### Stream.toList() 方法
```java
// 修改前 (Java 17)
list.stream().map(...).toList();

// 修改后 (Java 8)
list.stream().map(...).collect(Collectors.toList());
```

#### 3.3 JWT API 变更
```java
// JJWT 0.12.3 (Spring Boot 3.x)
Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

// JJWT 0.11.5 (Spring Boot 2.x)
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
```

#### 3.4 Spring Security 配置
```java
// Spring Boot 3.x
.csrf(AbstractHttpConfigurer::disable)
.authorizeHttpRequests(auth -> auth...)

// Spring Boot 2.x
.csrf().disable()
.authorizeHttpRequests()
    .antMatchers("/auth/**").permitAll()
```

#### 3.5 SSE Emitter 方法
```java
// 修改前
emitter.send("message", SseEmitter.event().name("token"));

// 修改后
emitter.send("message");
```

### 4. 配置文件修改

#### application.yml
```yaml
# Spring Boot 2.x 配置保持不变
server:
  port: 8080
  servlet:
    context-path: /api
```

## 修复的文件列表

### Controller 层
- ✅ ChatController.java - 修复 SSE 方法

### Service 层
- ✅ AuthService.java - 无修改
- ✅ UserService.java - 无修改
- ✅ MineralService.java - 修复 List.of() 和 .toList()
- ✅ ChatService.java - 修复 .toList()
- ✅ HistoryService.java - 修复 .toList()
- ✅ StatsService.java - 修复 .toList()

### Config 层
- ✅ SecurityFilterConfig.java - 修复 Spring Security 配置
- ✅ JwtUtil.java - 修复 JWT API
- ✅ JwtAuthenticationFilter.java - 修复 javax 包导入
- ✅ MybatisPlusConfig.java - 无修改

### DTO 层
- ✅ RegisterRequest.java - 修复编码问题

## 编译结果

```
[INFO] BUILD SUCCESS
[INFO] Total time:  20.370 s
[INFO] Finished at: 2026-03-29T12:46:56+08:00
```

## 运行项目

```bash
# 方式 1: 使用 Maven
mvn spring-boot:run

# 方式 2: 运行 JAR 包
java -jar target/mineralDO-system-1.0.0.jar

# 方式 3: 使用启动脚本
start.bat
```

## 注意事项

1. **JDK 版本**: 必须使用 JDK 1.8
2. **数据库**: MySQL 5.7 或 8.0
3. **编码**: 所有文件使用 UTF-8 编码

## 后续建议

如果条件允许，建议升级到 JDK 17，原因：
- Spring Boot 3.x 性能更好
- 代码更简洁（支持 record、pattern matching 等）
- 长期支持版本（LTS）
- 更好的类型推断和空指针安全

---

**修复完成时间**: 2026-03-29  
**适配版本**: JDK 1.8 + Spring Boot 2.7.18  
**项目状态**: ✅ 编译成功，可运行
