# 代码注释说明文档

## 已添加详细注释的文件

### 1. 配置类 (Config)

#### ✅ MybatisPlusConfig.java
- 类说明：MyBatis Plus 配置类
- 方法注释：
  - `mybatisPlusInterceptor()`: 配置分页插件
  - `insertFill()`: 插入时自动填充时间字段
  - `updateFill()`: 更新时自动填充时间字段

#### ✅ JwtUtil.java
- 类说明：JWT 工具类
- 属性注释：secret（密钥）、expiration（过期时间）、key（签名密钥）
- 方法注释：
  - `init()`: 初始化密钥
  - `generateToken()`: 生成 Token
  - `createToken()`: 创建 JWT
  - `parseToken()`: 解析 Token
  - `getUserIdFromToken()`: 获取用户 ID
  - `getUsernameFromToken()`: 获取用户名
  - `validateToken()`: 验证 Token

#### ✅ JwtAuthenticationFilter.java
- 类说明：JWT 认证过滤器
- 属性注释：jwtUtil（JWT 工具类）
- 方法注释：
  - `doFilterInternal()`: 过滤请求并验证 Token
  - `getTokenFromRequest()`: 从请求头获取 Token

#### ✅ SecurityFilterConfig.java
- 类说明：Spring Security 配置类
- 属性注释：jwtAuthenticationFilter（JWT 过滤器）
- 方法注释：
  - `securityFilterChain()`: 配置安全过滤器链
  - `authenticationManager()`: 配置认证管理器

### 2. 服务类 (Service)

#### ✅ AuthService.java
- 类说明：认证服务类
- 属性注释：userMapper、passwordEncoder、jwtUtil
- 方法注释：
  - `register()`: 用户注册（检查重复、加密密码）
  - `login()`: 用户登录（验证密码、生成 Token）
  - `logout()`: 用户登出

#### ✅ UserService.java
- 类说明：用户服务类
- 属性注释：userMapper、passwordEncoder、dateFormatter
- 方法注释：
  - `getProfile()`: 获取用户信息
  - `updateProfile()`: 更新用户信息（检查邮箱重复）
  - `updatePassword()`: 修改密码（验证原密码）

#### ✅ MineralService.java
- 类说明：矿物识别服务类
- 属性注释：detectionMapper、detectionResultMapper、mineralMapper、uploadDir、objectMapper
- 方法注释：
  - `detectMineral()`: 矿物识别（文件验证、保存、模拟识别）
  - `getDetectionDetail()`: 获取识别详情
  - `getCategories()`: 矿物分类统计
  - `getMineralInfo()`: 获取矿物详细信息
  - `validateFile()`: 验证文件
  - `saveFile()`: 保存文件
  - `simulateDetection()`: 模拟识别
  - `buildDetectionResponse()`: 构建响应

#### ✅ ChatService.java
- 类说明：聊天会话服务类
- 属性注释：chatSessionMapper、chatMessageMapper、detectionMapper
- 方法注释：
  - `createSession()`: 创建聊天会话
  - `getSessions()`: 分页获取会话列表
  - `getSessionMessages()`: 获取会话消息
  - `deleteSession()`: 删除会话及消息
  - `convertToResponse()`: 转换为响应对象
  - `convertToMessageResponse()`: 转换为消息响应对象

#### ✅ HistoryService.java
- 类说明：历史记录服务类
- 属性注释：detectionMapper、detectionResultMapper、mineralMapper、chatSessionMapper、dateFormatter
- 方法注释：
  - `getDetectionHistory()`: 获取识别历史（支持关键词、日期筛选）
  - `getDetectIdsByKeyword()`: 根据关键词获取识别记录 ID
  - `deleteDetectionRecord()`: 删除识别记录（级联删除）
  - `getChatHistory()`: 获取聊天历史
  - `convertToHistoryResponse()`: 转换为历史响应对象

#### ✅ StatsService.java
- 类说明：统计分析服务类
- 属性注释：detectionMapper、detectionResultMapper、chatSessionMapper
- 方法注释：
  - `getStatsOverview()`: 获取统计概览
  - `getMineralFrequency()`: 获取矿物识别频率统计
  - `getTopMineral()`: 获取最常识别的矿物
  - `getWeeklyActiveDays()`: 获取近 7 天活跃天数

## 注释规范

### 类注释
```java
/**
 * 类名
 * 功能描述
 */
```

### 方法注释
```java
/**
 * 方法功能说明
 * 
 * @param 参数名 参数说明
 * @return 返回值说明
 * @throws 异常类型 异常说明
 */
```

### 属性注释
```java
/**
 * 属性说明
 */
private String fieldName;
```

### 行内注释
```java
// 简短说明
```

## 关键业务逻辑注释示例

### 1. JWT Token 生成流程
```java
// 1. 创建 JWT 声明（包含用户信息）
Map<String, Object> claims = new HashMap<>();
claims.put("userId", userId);      // 用户 ID
claims.put("username", username);  // 用户名

// 2. 构建 JWT
return Jwts.builder()
        .setClaims(claims)                      // 设置声明
        .setSubject(subject)                    // 设置主题
        .setIssuedAt(now)                       // 设置签发时间
        .setExpiration(expirationDate)          // 设置过期时间
        .signWith(key, SignatureAlgorithm.HS256) // 使用 HS256 算法签名
        .compact();                             // 压缩 JWT
```

### 2. 用户注册业务逻辑
```java
// 1. 检查用户名或邮箱是否已存在
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUsername, request.getUsername())
        .or()
        .eq(User::getEmail, request.getEmail());

// 2. 如果存在，抛出相应异常
if (existingUserDO != null) {
    if (existingUserDO.getUsername().equals(request.getUsername())) {
        throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
    }
    if (existingUserDO.getEmail().equals(request.getEmail())) {
        throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已存在");
    }
}

// 3. 创建新用户（密码加密）
User userDO = new User();
userDO.setPasswordHash(passwordEncoder.encode(request.getPassword()));

// 4. 插入数据库
userMapper.insert(userDO);
```

### 3. Spring Security 配置流程
```java
http
    // 1. 禁用 CSRF（JWT 不需要）
    .csrf().disable()
    // 2. 配置无状态会话（不使用 Session）
    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    .and()
    // 3. 配置请求授权规则
    .authorizeHttpRequests()
        // 允许匿名访问的接口
        .antMatchers("/auth/**").permitAll()
        .antMatchers("/mineralDO/info/**").permitAll()
        .antMatchers("/mineralDO/categories").permitAll()
        // 其他请求需要认证
        .anyRequest().authenticated()
    .and()
    // 4. 添加 JWT 过滤器
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

## 未添加注释但代码清晰的文件

以下文件由于代码简单直观，未添加详细注释：

### 实体类 (Entity)
- User.java
- Detection.java
- DetectionResult.java
- Mineral.java
- ChatSession.java
- ChatMessage.java

### Mapper 接口
- UserMapper.java
- DetectionMapper.java
- DetectionResultMapper.java
- MineralMapper.java
- ChatSessionMapper.java
- ChatMessageMapper.java

### DTO 类
- 所有 Request 和 Response 类

### Controller 类
- 方法调用简单，业务逻辑在 Service 层

## 注释覆盖的模块

✅ **认证模块**: 100% 覆盖（AuthService）
✅ **配置模块**: 100% 覆盖（JwtUtil、JwtAuthenticationFilter、SecurityFilterConfig、MybatisPlusConfig）
✅ **用户模块**: 100% 覆盖（UserService）
✅ **矿物识别模块**: 100% 覆盖（MineralService）
✅ **聊天模块**: 100% 覆盖（ChatService）
✅ **历史记录模块**: 100% 覆盖（HistoryService）
✅ **统计模块**: 100% 覆盖（StatsService）

## 后续工作

可选优化：
- 为 Controller 层添加简要注释
- 为 Entity 和 DTO 类添加字段注释
- 为异常处理类添加注释

---

**注释完成时间**: 2026-03-29  
**已注释文件数**: 10 个核心 Service 和 Config 文件  
**注释覆盖率**: 约 80%（核心业务逻辑）  
**编译状态**: ✅ BUILD SUCCESS
