# 企业内部知识库（Wiki）系统 - API接口文档

## 一、文档说明

### （一）接口概述

本文档描述了Wiki系统的所有HTTP API接口和WebSocket接口。系统采用RESTful风格设计，所有接口返回统一的JSON格式。

### （二）基本信息

- 基础URL：`http://localhost:8080`
- 接口协议：HTTP/HTTPS
- 数据格式：JSON
- 字符编码：UTF-8

### （三）认证方式

除了公开接口外，所有接口都需要在请求头中携带JWT Token：

```
Authorization: Bearer <token>
```

Token有效期为7天，过期后需要重新登录获取。

### （四）统一响应格式

所有接口返回的数据格式统一如下：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {}
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 请求是否成功 |
| code | string | 状态码 |
| message | string | 提示信息 |
| data | object | 返回的数据 |

### （五）错误码说明

| 错误码 | 说明 |
|--------|------|
| BAD_REQUEST | 请求参数错误 |
| UNAUTHORIZED | 未登录或Token失效 |
| FORBIDDEN | 无权限访问 |
| NOT_FOUND | 资源不存在 |
| VALIDATION_FAILED | 数据验证失败 |
| VERIFY_CODE_INVALID | 验证码错误或已过期 |
| RATE_LIMITED | 请求频率超限 |
| USER_ALREADY_EXISTS | 用户已存在 |
| DOC_CONFLICT | 文档编辑冲突 |
| EDIT_LOCKED | 文档被他人锁定 |
| INTERNAL_ERROR | 服务器内部错误 |

---

## 二、用户认证接口

### （一）发送验证码

用于注册时发送验证码到邮箱或手机。

**接口地址：** `POST /api/auth/send-code`

**是否需要认证：** 否

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 验证码类型：EMAIL（邮箱）或 PHONE（手机） |
| target | string | 是 | 邮箱地址或手机号 |

**请求示例：**

```json
{
  "type": "EMAIL",
  "target": "user@example.com"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "testMode": true,
    "testCode": "123456"
  }
}
```

**说明：**
- 同一IP/邮箱/手机号1分钟内只能请求1次
- 验证码有效期5分钟
- 如果未配置邮件/短信服务，系统进入测试模式，直接返回验证码

### （二）用户注册

**接口地址：** `POST /api/auth/register`

**是否需要认证：** 否

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20个字符 |
| password | string | 是 | 密码，8-32个字符 |
| email | string | 否 | 邮箱地址 |
| phone | string | 否 | 手机号 |
| contact | string | 否 | 联系方式（email/phone二选一） |
| code | string | 是 | 验证码 |
| avatarUrl | string | 否 | 头像URL |

**请求示例：**

```json
{
  "username": "zhangsan",
  "password": "Pass1234",
  "email": "zhangsan@example.com",
  "code": "123456",
  "avatarUrl": "/api/public/avatars/default.png"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1234567890,
    "username": "zhangsan",
    "role": "USER",
    "avatarUrl": "/api/public/avatars/default.png"
  }
}
```

### （三）用户登录

**接口地址：** `POST /api/auth/login`

**是否需要认证：** 否

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | string | 是 | 用户名/邮箱/手机号 |
| password | string | 是 | 密码 |

**请求示例：**

```json
{
  "account": "zhangsan",
  "password": "Pass1234"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1234567890,
    "username": "zhangsan",
    "role": "USER",
    "avatarUrl": "/api/public/avatars/avatar_123.png"
  }
}
```

### （四）获取当前用户信息

**接口地址：** `GET /api/auth/me`

**是否需要认证：** 是

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "userId": 1234567890,
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatarUrl": "/api/public/avatars/avatar_123.png",
    "role": "USER",
    "createdAt": "2026-03-15T10:30:00"
  }
}
```

### （五）上传头像

**接口地址：** `POST /api/auth/upload-avatar`

**是否需要认证：** 是

**请求类型：** `multipart/form-data`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件，最大2MB |

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "avatarUrl": "/api/public/avatars/avatar_123.png"
  }
}
```

### （六）修改个人信息

**接口地址：** `PUT /api/auth/profile`

**是否需要认证：** 是

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 新用户名 |
| avatarUrl | string | 否 | 新头像URL |
| email | string | 否 | 新邮箱 |
| phone | string | 否 | 新手机号 |
| oldCode | string | 否 | 旧邮箱/手机验证码 |
| newCode | string | 否 | 新邮箱/手机验证码 |

**请求示例：**

```json
{
  "username": "zhangsan_new",
  "avatarUrl": "/api/public/avatars/avatar_456.png"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "userId": 1234567890,
    "username": "zhangsan_new",
    "email": "zhangsan@example.com",
    "avatarUrl": "/api/public/avatars/avatar_456.png",
    "role": "USER"
  }
}
```

### （七）发送修改信息验证码

**接口地址：** `POST /api/auth/send-update-code`

**是否需要认证：** 是

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 验证码类型：EMAIL 或 PHONE |
| target | string | 是 | 邮箱地址或手机号 |
| isOld | boolean | 是 | 是否为旧联系方式 |

**请求示例：**

```json
{
  "type": "EMAIL",
  "target": "newemail@example.com",
  "isOld": false
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "testMode": true,
    "testCode": "654321"
  }
}
```

---

## 三、知识库管理接口

### （一）创建知识库

**接口地址：** `POST /api/kbs`

**是否需要认证：** 是

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 知识库名称，1-50个字符 |
| type | string | 是 | 类型：PUBLIC（公开）、PRIVATE（私有）、TEAM（团队） |
| description | string | 否 | 描述，最多200字 |

**请求示例：**

```json
{
  "name": "技术文档库",
  "type": "TEAM",
  "description": "团队技术文档集合"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "id": 9876543210,
    "name": "技术文档库",
    "type": "TEAM",
    "description": "团队技术文档集合",
    "creatorId": 1234567890,
    "createdAt": "2026-03-16T14:20:00"
  }
}
```

### （二）获取我的知识库列表

**接口地址：** `GET /api/kbs/mine`

**是否需要认证：** 是

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": [
    {
      "id": 9876543210,
      "name": "技术文档库",
      "type": "TEAM",
      "description": "团队技术文档集合",
      "role": "ADMIN",
      "createdAt": "2026-03-16T14:20:00"
    },
    {
      "id": 9876543211,
      "name": "个人笔记",
      "type": "PRIVATE",
      "description": null,
      "role": "ADMIN",
      "createdAt": "2026-03-15T10:00:00"
    }
  ]
}
```

### （三）邀请或更新成员

**接口地址：** `POST /api/kbs/{kbId}/members`

**是否需要认证：** 是

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | long | 知识库ID |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| usernameOrEmail | string | 是 | 用户名或邮箱 |
| role | string | 是 | 角色：READER（只读）、EDITOR（编辑）、ADMIN（管理员） |

**请求示例：**

```json
{
  "usernameOrEmail": "lisi",
  "role": "EDITOR"
}
```

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "userId": 1234567891,
    "username": "lisi",
    "avatarUrl": "/api/public/avatars/avatar_789.png",
    "role": "EDITOR",
    "joinedAt": "2026-03-16T15:00:00"
  }
}
```

### （四）获取知识库成员列表

**接口地址：** `GET /api/kbs/{kbId}/members`

**是否需要认证：** 是

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | long | 知识库ID |

**响应示例：**

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": [
    {
      "userId": 1234567890,
      "username": "zhangsan",
      "avatarUrl": "/api/public/avatars/avatar_123.png",
      "role": "ADMIN",
      "joinedAt": "2026-03-16T14:20:00"
    },
    {
      "userId": 1234567891,
      "username": "lisi",
      "avatarUrl": "/api/public/avatars/avatar_789.png",
      "role": "EDITOR",
      "joinedAt": "2026-03-16T15:00:00"
    }
  ]
}
```

---

## 四、文档管理接口

### （一）创建文档

**接口地址：** `POST /api/docs`

**是否需要认证：** 是
