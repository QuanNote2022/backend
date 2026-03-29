# 矿物识别系统 - 后端接口规格说明书

## 目录

- [1. 接口规范](#1-接口规范)
- [2. 认证模块](#2-认证模块)
- [3. 用户模块](#3-用户模块)
- [4. 矿物识别模块](#4-矿物识别模块)
- [5. 聊天会话模块](#5-聊天会话模块)
- [6. 历史记录模块](#6-历史记录模块)
- [7. 统计模块](#7-统计模块)
- [8. 错误码说明](#8-错误码说明)

---

## 1. 接口规范

### 1.1 基础信息

- **基础路径**: `/api`
- **数据格式**: JSON
- **字符编码**: UTF-8

### 1.2 统一响应格式

所有接口返回统一使用以下格式：

```typescript
interface ApiResponse<T> {
  code: number      // 状态码，0 表示成功
  message: string   // 响应消息
  data: T          // 响应数据
}
```

### 1.3 分页查询参数

```typescript
interface PageQuery {
  page?: number      // 页码，默认 1
  pageSize?: number  // 每页数量，默认 10
}
```

### 1.4 分页响应格式

```typescript
interface PageResult<T> {
  list: T[]      // 数据列表
  total: number  // 总记录数
  page: number   // 当前页码
  pageSize: number  // 每页数量
}
```

### 1.5 认证方式

- 使用 JWT Token 进行认证
- Token 放在请求头中：`Authorization: Bearer <token>`
- Token 有效期：默认 24 小时

---

## 2. 认证模块

### 2.1 用户注册

**接口**: `POST /auth/register`

**描述**: 注册新用户

**请求参数**:

```json
{
  "username": "string",     // 用户名，必填，3-20 个字符
  "password": "string",     // 密码，必填，6-20 个字符
  "email": "string"         // 邮箱，必填，有效邮箱格式
}
```

**响应示例**:

```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "userId": "usr_123456789"
  }
}
```

**业务逻辑**:
1. 验证用户名、密码、邮箱格式
2. 检查用户名和邮箱是否已存在
3. 对密码进行加密存储（建议使用 bcrypt）
4. 创建用户记录
5. 返回用户 ID

---

### 2.2 用户登录

**接口**: `POST /auth/login`

**描述**: 用户登录获取 Token

**请求参数**:

```json
{
  "username": "string",   // 用户名，必填
  "password": "string"    // 密码，必填
}
```

**响应示例**:

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

**业务逻辑**:
1. 验证用户名和密码
2. 密码验证通过后生成 JWT Token
3. Token 包含用户 ID、用户名等信息
4. 设置 Token 过期时间（默认 24 小时）

---

### 2.3 用户登出

**接口**: `POST /auth/logout`

**描述**: 用户登出，使当前 Token 失效

**请求头**:
```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 0,
  "message": "登出成功",
  "data": null
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 将 Token 加入黑名单（可选，使用 Redis 等缓存）
3. 或清除服务端会话记录

---

## 3. 用户模块

### 3.1 获取用户信息

**接口**: `GET /user/profile`

**描述**: 获取当前登录用户的详细信息

**请求头**:
```
Authorization: Bearer <token>
```

**响应示例**:

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

**业务逻辑**:
1. 从 Token 中解析用户 ID
2. 查询用户信息
3. 返回用户公开信息

---

### 3.2 更新用户信息

**接口**: `PUT /user/profile`

**描述**: 更新当前用户的基本信息

**请求头**:
```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "email": "string",      // 可选，有效邮箱格式
  "avatar": "string",     // 可选，头像 URL
  "nickname": "string"    // 可选，昵称，1-20 个字符
}
```

**响应示例**:

```json
{
  "code": 0,
  "message": "更新成功",
  "data": {
    "userId": "usr_123456789",
    "username": "zhangsan",
    "email": "newemail@example.com",
    "avatar": "https://example.com/avatars/new.jpg",
    "nickname": "新昵称",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 验证参数格式
3. 检查邮箱是否已被其他用户使用
4. 更新用户信息
5. 返回更新后的用户信息

---

### 3.3 修改密码

**接口**: `PUT /user/password`

**描述**: 修改当前用户的登录密码

**请求头**:
```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "oldPassword": "string",   // 原密码，必填
  "newPassword": "string"    // 新密码，必填，6-20 个字符
}
```

**响应示例**:

```json
{
  "code": 0,
  "message": "密码修改成功",
  "data": null
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 验证原密码是否正确
3. 验证新密码格式
4. 加密新密码并更新
5. 可选：使当前 Token 失效，要求重新登录

---

## 4. 矿物识别模块

### 4.1 矿物识别

**接口**: `POST /mineral/detect`

**描述**: 上传矿物图片进行识别

**请求头**:
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**请求参数** (FormData):

```
image: File    // 图片文件，必填，支持 JPG/PNG 格式，最大 10MB
```

**响应示例**:

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

**业务逻辑**:
1. 验证 Token 有效性
2. 接收并验证上传的图片
3. 保存图片到存储系统（本地或对象存储）
4. 调用 AI 模型进行矿物识别（YOLO 等）
5. 获取识别结果（标签、置信度、边界框）
6. 查询矿物详细信息
7. 保存识别记录到数据库
8. 返回识别结果

**技术要点**:
- 图片格式验证：JPG、PNG
- 图片大小限制：最大 10MB
- 图片压缩处理（可选）
- AI 模型集成（YOLOv8 等）
- 边界框坐标格式：[x_min, y_min, x_max, y_max]

---

### 4.2 获取识别详情

**接口**: `GET /mineral/detect/{detectId}`

**描述**: 根据 ID 获取矿物识别的详细信息

**请求头**:
```
Authorization: Bearer <token>
```

**路径参数**:
```
detectId: string    // 识别记录 ID
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
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

**业务逻辑**:
1. 验证 Token 有效性
2. 根据 detectId 查询识别记录
3. 验证记录归属（只能查看自己的记录）
4. 返回识别详情

---

### 4.3 获取矿物分类列表

**接口**: `GET /mineral/categories`

**描述**: 获取所有矿物分类及其数量统计

**请求头**:
```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
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
    },
    {
      "id": "cat_003",
      "name": "云母",
      "count": 5
    }
  ]
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 查询当前用户识别过的矿物分类
3. 统计每种矿物的识别次数
4. 返回分类列表

---

### 4.4 获取矿物详细信息

**接口**: `GET /mineral/info/{mineralName}`

**描述**: 根据矿物名称获取详细信息

**请求头**:
```
Authorization: Bearer <token>
```

**路径参数**:
```
mineralName: string    // 矿物名称（URL 编码）
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
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

**业务逻辑**:
1. 验证 Token 有效性
2. 根据矿物名称查询矿物数据库
3. 返回矿物详细信息

**数据来源**:
- 预置的矿物数据库（MySQL/PostgreSQL）
- 或调用外部矿物学 API

---

## 5. 聊天会话模块

### 5.1 创建聊天会话

**接口**: `POST /chat/session`

**描述**: 创建新的聊天会话，可关联矿物识别记录

**请求头**:
```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "detectId": "string",      // 可选，关联的识别记录 ID
  "mineralName": "string"    // 可选，矿物名称
}
```

**响应示例**:

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

**业务逻辑**:
1. 验证 Token 有效性
2. 如果提供了 detectId，验证识别记录存在且归属当前用户
3. 根据矿物名称生成会话标题
4. 创建会话记录
5. 返回会话信息

---

### 5.2 获取会话列表

**接口**: `GET /chat/sessions`

**描述**: 获取当前用户的聊天会话列表（分页）

**请求头**:
```
Authorization: Bearer <token>
```

**查询参数**:

```
page: number       // 可选，页码，默认 1
pageSize: number   // 可选，每页数量，默认 10
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "sessionId": "sess_123456789",
        "title": "石英矿物咨询",
        "mineralName": "石英",
        "messageCount": 5,
        "lastActiveAt": "2024-01-01T12:30:00Z",
        "createdAt": "2024-01-01T12:00:00Z"
      }
    ],
    "total": 20,
    "page": 1,
    "pageSize": 10
  }
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 查询当前用户的会话列表
3. 按最后活跃时间倒序排序
4. 返回分页数据

---

### 5.3 获取会话消息

**接口**: `GET /chat/session/{sessionId}/messages`

**描述**: 获取指定会话的所有消息

**请求头**:
```
Authorization: Bearer <token>
```

**路径参数**:
```
sessionId: string    // 会话 ID
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
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

**业务逻辑**:
1. 验证 Token 有效性
2. 验证会话归属
3. 查询会话下的所有消息
4. 按创建时间正序返回

---

### 5.4 发送消息

**接口**: `POST /chat/session/{sessionId}/send`

**描述**: 向指定会话发送消息（支持 SSE 流式响应）

**请求头**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数**:
```
sessionId: string    // 会话 ID
```

**请求参数**:

```json
{
  "content": "string",           // 消息内容，必填
  "mineralContext": "string"     // 可选，矿物上下文信息
}
```

**响应示例** (SSE 流式):

```
data: {"token": "石", "done": false}

data: {"token": "英", "done": false}

data: {"token": "的", "done": false}

data: {"token": "莫", "done": false}

data: {"token": "氏", "done": false}

data: {"token": "硬", "done": false}

data: {"token": "度", "done": false}

data: {"token": "为", "done": false}

data: {"token": "7", "done": false}

data: {"token": "。", "done": false}

data: {"done": true, "messageId": "msg_003"}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 验证会话归属
3. 保存用户消息
4. 调用大语言模型 API（如 ChatGLM、Qwen 等）
5. 使用 SSE 流式返回 AI 响应
6. 保存 AI 回复消息
7. 更新会话的最后活跃时间和消息数

**技术要点**:
- 使用 Server-Sent Events (SSE) 实现流式响应
- Content-Type: `text/event-stream`
- 每个 token 单独推送
- 最后推送 done 标记和 messageId

---

### 5.5 删除会话

**接口**: `DELETE /chat/session/{sessionId}`

**描述**: 删除指定的聊天会话

**请求头**:
```
Authorization: Bearer <token>
```

**路径参数**:
```
sessionId: string    // 会话 ID
```

**响应示例**:

```json
{
  "code": 0,
  "message": "删除成功",
  "data": null
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 验证会话归属
3. 级联删除会话下的所有消息
4. 删除会话记录

---

## 6. 历史记录模块

### 6.1 获取识别历史

**接口**: `GET /history/detections`

**描述**: 获取用户的矿物识别历史记录（分页、筛选）

**请求头**:
```
Authorization: Bearer <token>
```

**查询参数**:

```
page: number       // 可选，页码，默认 1
pageSize: number   // 可选，每页数量，默认 10
keyword: string    // 可选，搜索关键词（矿物名称）
startDate: string  // 可选，开始日期（ISO 8601 格式）
endDate: string    // 可选，结束日期（ISO 8601 格式）
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
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
    ],
    "total": 50,
    "page": 1,
    "pageSize": 10
  }
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 根据条件筛选识别记录
3. 支持关键词模糊匹配矿物名称
4. 支持日期范围筛选
5. 返回分页数据

---

### 6.2 删除识别记录

**接口**: `DELETE /history/detections/{detectId}`

**描述**: 删除指定的矿物识别记录

**请求头**:
```
Authorization: Bearer <token>
```

**路径参数**:
```
detectId: string    // 识别记录 ID
```

**响应示例**:

```json
{
  "code": 0,
  "message": "删除成功",
  "data": null
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 验证记录归属
3. 删除识别记录
4. 可选：同时删除关联的图片文件

---

### 6.3 获取聊天历史

**接口**: `GET /history/chats`

**描述**: 获取用户的聊天会话历史记录（分页）

**请求头**:
```
Authorization: Bearer <token>
```

**查询参数**:

```
page: number       // 可选，页码，默认 1
pageSize: number   // 可选，每页数量，默认 10
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "sessionId": "sess_123456789",
        "title": "石英矿物咨询",
        "mineralName": "石英",
        "messageCount": 5,
        "lastActiveAt": "2024-01-01T12:30:00Z",
        "createdAt": "2024-01-01T12:00:00Z"
      }
    ],
    "total": 20,
    "page": 1,
    "pageSize": 10
  }
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 查询用户的所有会话
3. 按创建时间倒序排序
4. 返回分页数据

---

## 7. 统计模块

### 7.1 获取统计概览

**接口**: `GET /stats/overview`

**描述**: 获取用户的使用统计概览数据

**请求头**:
```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalDetections": 50,
    "totalChats": 30,
    "topMineral": "石英",
    "weeklyActiveDays": 5
  }
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 统计用户总识别次数
3. 统计用户总会话数
4. 查询识别次数最多的矿物
5. 统计最近 7 天活跃天数
6. 返回统计数据

---

### 7.2 获取矿物识别频率

**接口**: `GET /stats/mineral-frequency`

**描述**: 获取用户矿物识别频率统计

**请求头**:
```
Authorization: Bearer <token>
```

**查询参数**:

```
days: number    // 可选，统计天数，默认 30
```

**响应示例**:

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "mineralName": "石英",
      "count": 15
    },
    {
      "mineralName": "长石",
      "count": 8
    },
    {
      "mineralName": "云母",
      "count": 5
    },
    {
      "mineralName": "方解石",
      "count": 3
    }
  ]
}
```

**业务逻辑**:
1. 验证 Token 有效性
2. 查询指定天数内的识别记录
3. 按矿物名称分组统计
4. 按识别次数降序排序
5. 返回统计结果

---

## 8. 错误码说明

### 8.1 HTTP 状态码

- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `204 No Content`: 删除成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证或 Token 无效
- `403 Forbidden`: 无权限访问
- `404 Not Found`: 资源不存在
- `500 Internal Server Error`: 服务器内部错误

### 8.2 业务错误码

```typescript
// 通用错误
0: "成功"
1000: "参数错误"
1001: "请求格式错误"
1002: "数据不存在"
1003: "数据已存在"

// 认证错误
2000: "未登录"
2001: "Token 无效"
2002: "Token 已过期"
2003: "用户名或密码错误"
2004: "用户已被禁用"

// 用户错误
3000: "用户名已存在"
3001: "邮箱已存在"
3002: "原密码错误"
3003: "新密码格式错误"

// 矿物识别错误
4000: "图片格式不支持"
4001: "图片大小超限"
4002: "识别失败"
4003: "识别记录不存在"

// 聊天错误
5000: "会话不存在"
5001: "会话已删除"
5002: "消息内容为空"
5003: "AI 服务不可用"

// 上传错误
6000: "文件上传失败"
6001: "文件类型不支持"
6002: "文件大小超限"
```

### 8.3 错误响应示例

```json
{
  "code": 2003,
  "message": "用户名或密码错误",
  "data": null
}
```

---

## 9. 数据库设计建议

### 9.1 用户表 (users)

```sql
CREATE TABLE users (
  user_id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(32) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(128) UNIQUE NOT NULL,
  avatar VARCHAR(512),
  nickname VARCHAR(32),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 9.2 识别记录表 (detections)

```sql
CREATE TABLE detections (
  detect_id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  image_url VARCHAR(512) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  INDEX idx_user_created (user_id, created_at)
);
```

### 9.3 识别结果表 (detection_results)

```sql
CREATE TABLE detection_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  detect_id VARCHAR(64) NOT NULL,
  label VARCHAR(64) NOT NULL,
  confidence DECIMAL(5,4) NOT NULL,
  bbox_x1 INT,
  bbox_y1 INT,
  bbox_x2 INT,
  bbox_y2 INT,
  FOREIGN KEY (detect_id) REFERENCES detections(detect_id),
  INDEX idx_detect_id (detect_id)
);
```

### 9.4 矿物信息表 (minerals)

```sql
CREATE TABLE minerals (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL,
  formula VARCHAR(128),
  hardness VARCHAR(16),
  luster VARCHAR(32),
  color VARCHAR(64),
  origin VARCHAR(256),
  uses VARCHAR(512),
  description TEXT,
  thumbnail VARCHAR(512)
);
```

### 9.5 聊天会话表 (chat_sessions)

```sql
CREATE TABLE chat_sessions (
  session_id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  mineral_name VARCHAR(64),
  detect_id VARCHAR(64),
  message_count INT DEFAULT 0,
  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (detect_id) REFERENCES detections(detect_id),
  INDEX idx_user_last_active (user_id, last_active_at)
);
```

### 9.6 聊天消息表 (chat_messages)

```sql
CREATE TABLE chat_messages (
  message_id VARCHAR(64) PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL,
  role ENUM('user', 'assistant') NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
  INDEX idx_session_created (session_id, created_at)
);
```

---

## 10. 技术栈建议

### 10.1 后端框架

- **Python**: FastAPI / Flask / Django
- **Node.js**: Express / NestJS / Koa
- **Java**: Spring Boot

### 10.2 数据库

- **关系型**: MySQL / PostgreSQL
- **缓存**: Redis（用于 Token 黑名单、会话缓存）

### 10.3 AI 模型

- **目标检测**: YOLOv8 / YOLOv5
- **大语言模型**: ChatGLM / Qwen / Baichuan

### 10.4 文件存储

- **本地存储**: 适用于开发环境
- **对象存储**: AWS S3 / 阿里云 OSS / 七牛云（适用于生产环境）

### 10.5 部署

- **容器化**: Docker + Docker Compose
- **Web 服务器**: Nginx（反向代理）
- **进程管理**: Gunicorn / Uvicorn（Python）或 PM2（Node.js）

---

## 11. 安全建议

1. **密码安全**: 使用 bcrypt 或 argon2 加密存储
2. **Token 安全**: JWT Token 设置合理过期时间，支持刷新机制
3. **输入验证**: 对所有输入参数进行严格验证
4. **文件上传**: 限制文件类型、大小，进行病毒扫描
5. **CORS 配置**: 仅允许前端域名访问
6. **速率限制**: 对 API 接口进行限流，防止滥用
7. **日志记录**: 记录关键操作日志，便于审计
8. **HTTPS**: 生产环境必须使用 HTTPS

---

## 12. 开发建议

### 12.1 开发环境配置

```bash
# .env.development
API_BASE_URL=/api
ENABLE_MOCK=true
DEBUG=true
```

### 12.2 生产环境配置

```bash
# .env.production
API_BASE_URL=https://api.example.com
ENABLE_MOCK=false
DEBUG=false
```

### 12.3 API 版本控制

建议使用 URL 路径版本控制：
- `/api/v1/auth/login`
- `/api/v2/auth/login`

---

**文档版本**: v1.0  
**最后更新**: 2026-03-29  
**维护者**: 开发团队
