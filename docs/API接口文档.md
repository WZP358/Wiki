# 企业内部知识库（Wiki）系统 - API接口文档

## 一、文档说明

### （一）接口概述

本文档描述了Wiki系统的HTTP API接口和WebSocket接口。系统采用RESTful风格设计，所有接口返回统一的JSON格式。

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

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {}
}
```

### （五）错误码

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

## 二、用户认证接口

### （一）发送验证码

**接口：** `POST /api/auth/send-code`

**认证：** 否

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | EMAIL或PHONE |
| target | string | 是 | 邮箱地址或手机号 |

**响应：**

```json
{
  "success": true,
  "data": {
    "testMode": true,
    "testCode": "123456"
  }
}
```

### （二）用户注册

**接口：** `POST /api/auth/register`

**认证：** 否

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20个字符 |
| password | string | 是 | 密码，8-32个字符 |
| email | string | 否 | 邮箱地址 |
| phone | string | 否 | 手机号 |
| code | string | 是 | 验证码 |
| avatarUrl | string | 否 | 头像路径 |

**响应：**

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "1234567890",
    "username": "zhangsan",
    "role": "USER"
  }
}
```

### （三）用户登录

**接口：** `POST /api/auth/login`

**认证：** 否

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | string | 是 | 用户名/邮箱/手机号 |
| password | string | 是 | 密码 |

**响应：**

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "1234567890",
    "username": "zhangsan",
    "role": "USER"
  }
}
```

### （四）获取当前用户信息

**接口：** `GET /api/auth/me`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": {
    "userId": "1234567890",
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

**接口：** `POST /api/auth/upload-avatar`

**认证：** 是

**请求类型：** `multipart/form-data`

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件，最大2MB |

**响应：**

```json
{
  "success": true,
  "data": {
    "avatarUrl": "/api/public/avatars/avatar_123.png"
  }
}
```

### （六）修改个人信息

**接口：** `PUT /api/auth/profile`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 新用户名 |
| avatarUrl | string | 否 | 新头像路径 |
| email | string | 否 | 新邮箱 |
| phone | string | 否 | 新手机号 |
| oldCode | string | 否 | 旧邮箱/手机验证码 |
| newCode | string | 否 | 新邮箱/手机验证码 |

## 三、知识库管理接口

### （一）创建知识库

**接口：** `POST /api/kbs`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 知识库名称，1-50个字符 |
| type | string | 是 | PUBLIC/PRIVATE/TEAM |
| description | string | 否 | 描述，最多200字 |

**响应：**

```json
{
  "success": true,
  "data": {
    "id": "9876543210",
    "name": "技术文档库",
    "type": "TEAM",
    "description": "团队技术文档集合",
    "creatorId": "1234567890",
    "createdAt": "2026-03-16T14:20:00"
  }
}
```

### （二）获取我的知识库列表

**接口：** `GET /api/kbs/mine`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": [
    {
      "id": "9876543210",
      "name": "技术文档库",
      "type": "TEAM",
      "description": "团队技术文档集合",
      "role": "ADMIN",
      "createdAt": "2026-03-16T14:20:00"
    }
  ]
}
```

### （三）邀请或更新成员

**接口：** `POST /api/kbs/{kbId}/members`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| usernameOrEmail | string | 是 | 用户名或邮箱 |
| role | string | 是 | VIEWER/EDITOR/ADMIN |

**响应：**

```json
{
  "success": true,
  "data": {
    "userId": "1234567891",
    "username": "lisi",
    "avatarUrl": "/api/public/avatars/avatar_789.png",
    "role": "EDITOR",
    "joinedAt": "2026-03-16T15:00:00"
  }
}
```

### （四）获取知识库成员列表

**接口：** `GET /api/kbs/{kbId}/members`

**认证：** 是

## 四、文档管理接口

### （一）创建文档

**接口：** `POST /api/docs`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kbId | string | 是 | 知识库ID |
| parentId | string | 否 | 父文档ID |
| title | string | 是 | 文档标题 |
| markdownContent | string | 否 | Markdown内容 |
| visibility | string | 否 | PUBLIC/TEAM/PRIVATE |

**响应：**

```json
{
  "success": true,
  "data": {
    "id": "1111111111",
    "kbId": "9876543210",
    "title": "新文档",
    "markdownContent": "# 标题\n\n内容...",
    "visibility": "TEAM",
    "createdAt": "2026-03-16T16:00:00"
  }
}
```

### （二）获取文档详情

**接口：** `GET /api/docs/{docId}`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": {
    "id": "1111111111",
    "kbId": "9876543210",
    "title": "新文档",
    "markdownContent": "# 标题\n\n内容...",
    "htmlContent": "<h1>标题</h1><p>内容...</p>",
    "visibility": "TEAM",
    "viewCount": 10,
    "versionNo": 1,
    "published": true,
    "createdAt": "2026-03-16T16:00:00",
    "updatedAt": "2026-03-16T16:30:00"
  }
}
```

### （三）更新文档

**接口：** `PUT /api/docs/{docId}`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 文档标题 |
| markdownContent | string | 否 | Markdown内容 |
| visibility | string | 否 | 可见性 |
| published | boolean | 否 | 是否发布 |

### （四）删除文档

**接口：** `DELETE /api/docs/{docId}`

**认证：** 是

### （五）获取文档版本列表

**接口：** `GET /api/docs/{docId}/versions`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": [
    {
      "id": "2222222222",
      "docId": "1111111111",
      "versionNo": 2,
      "title": "更新后的标题",
      "editorName": "zhangsan",
      "commitMessage": "更新内容",
      "createdAt": "2026-03-16T17:00:00"
    }
  ]
}
```

### （六）保存草稿

**接口：** `POST /api/docs/{docId}/draft`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 草稿标题 |
| markdownContent | string | 否 | 草稿内容 |

### （七）获取草稿

**接口：** `GET /api/docs/{docId}/draft`

**认证：** 是

## 五、文档模板接口

### （一）获取模板列表

**接口：** `GET /api/templates`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 分类筛选 |
| kbId | string | 否 | 知识库ID |

**响应：**

```json
{
  "success": true,
  "data": [
    {
      "id": "1",
      "name": "会议纪要",
      "description": "标准会议记录模板",
      "category": "会议",
      "isPublic": true,
      "useCount": 10,
      "createdAt": "2026-03-01T00:00:00"
    }
  ]
}
```

### （二）创建模板

**接口：** `POST /api/templates`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 模板名称 |
| description | string | 否 | 模板描述 |
| markdownContent | string | 是 | 模板内容 |
| category | string | 否 | 分类 |
| isPublic | boolean | 否 | 是否公开 |
| kbId | string | 否 | 知识库ID |

### （三）使用模板

**接口：** `POST /api/templates/{templateId}/use`

**认证：** 是

## 六、文档评论接口

### （一）获取评论列表

**接口：** `GET /api/docs/{docId}/comments`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": [
    {
      "id": "5555555555",
      "documentId": "1111111111",
      "authorId": "1234567890",
      "authorName": "zhangsan",
      "authorAvatar": "/api/public/avatars/avatar_123.png",
      "content": "这是一条评论",
      "parentId": null,
      "likeCount": 5,
      "isResolved": false,
      "createdAt": "2026-03-16T18:30:00",
      "replies": []
    }
  ]
}
```

### （二）创建评论

**接口：** `POST /api/docs/{docId}/comments`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 评论内容 |
| parentId | string | 否 | 父评论ID |

### （三）删除评论

**接口：** `DELETE /api/docs/{docId}/comments/{commentId}`

**认证：** 是

### （四）标记评论为已解决

**接口：** `POST /api/docs/{docId}/comments/{commentId}/resolve`

**认证：** 是

## 七、文档反应接口

### （一）获取反应统计

**接口：** `GET /api/docs/{docId}/reactions`

**认证：** 是

**响应：**

```json
{
  "success": true,
  "data": {
    "counts": {
      "LIKE": 10,
      "LOVE": 5,
      "CLAP": 3,
      "FIRE": 2
    },
    "userReacted": {
      "LIKE": true,
      "LOVE": false,
      "CLAP": false,
      "FIRE": false
    }
  }
}
```

### （二）切换反应

**接口：** `POST /api/docs/{docId}/reactions/{reactionType}`

**认证：** 是

**路径参数：** reactionType可选值：LIKE/LOVE/CLAP/FIRE

**响应：**

```json
{
  "success": true,
  "data": {
    "added": true
  }
}
```

## 八、分享链接接口

### （一）创建分享链接

**接口：** `POST /api/share`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| docId | string | 是 | 文档ID |
| expireAt | string | 否 | 过期时间（ISO 8601格式） |

**响应：**

```json
{
  "success": true,
  "data": {
    "id": "6666666666",
    "token": "abc123def456",
    "docId": "1111111111",
    "expireAt": "2026-03-23T00:00:00",
    "createdAt": "2026-03-16T19:00:00"
  }
}
```

### （二）通过分享链接访问文档

**接口：** `GET /api/share/{token}`

**认证：** 否

**响应：**

```json
{
  "success": true,
  "data": {
    "id": "1111111111",
    "title": "新文档",
    "htmlContent": "<h1>标题</h1><p>内容...</p>",
    "createdAt": "2026-03-16T16:00:00"
  }
}
```

## 九、搜索接口

### （一）搜索文档

**接口：** `GET /api/docs/search`

**认证：** 是

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词 |
| kbId | string | 否 | 知识库ID |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认20 |

**响应：**

```json
{
  "success": true,
  "data": {
    "total": 10,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": "1111111111",
        "title": "新文档",
        "kbId": "9876543210",
        "kbName": "技术文档库",
        "snippet": "...关键词...",
        "createdAt": "2026-03-16T16:00:00"
      }
    ]
  }
}
```

## 十、管理接口

### （一）获取操作日志

**接口：** `GET /api/admin/logs`

**认证：** 是（需要ADMIN角色）

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认20 |
| action | string | 否 | 操作类型筛选 |
| userId | string | 否 | 用户ID筛选 |

**响应：**

```json
{
  "success": true,
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": "7777777777",
        "userId": "1234567890",
        "username": "zhangsan",
        "action": "LOGIN",
        "targetType": "USER",
        "targetId": "1234567890",
        "ip": "192.168.1.1",
        "detail": "用户登录",
        "createdAt": "2026-03-16T20:00:00"
      }
    ]
  }
}
```

## 十一、WebSocket协议

### （一）连接端点

```
ws://localhost:8080/ws/collab/{docId}?token={jwt_token}
```

### （二）消息格式

**客户端发送：**

```json
{
  "type": "operation",
  "data": {
    "ops": [{"insert": "Hello"}],
    "baseVersion": 10
  }
}
```

**服务器广播：**

```json
{
  "type": "operation",
  "userId": "1234567890",
  "username": "zhangsan",
  "data": {
    "ops": [{"insert": "Hello"}],
    "version": 11
  }
}
```

### （三）消息类型

| 类型 | 说明 |
|------|------|
| join | 加入协作 |
| leave | 离开协作 |
| operation | 编辑操作 |
| cursor | 光标位置 |
| conflict | 冲突通知 |

## 十二、总结

本文档描述了企业内部知识库系统的所有API接口，包括用户认证、知识库管理、文档管理、模板管理、评论管理、反应管理、分享链接、搜索和管理等功能。所有接口遵循RESTful设计规范，使用统一的响应格式。

## ֪ʶ���ֶ���Ȩ�޹��򲹳�

��ǰ֪ʶ��ӿ�ͳһʹ�����¹ؼ��ֶΣ�

| �ֶ� | ���� | ˵�� |
|------|------|------|
| id | string | ֪ʶ�� ID |
| name | string | ֪ʶ������ |
| type | string | COMPANY/DEPARTMENT/PRIVATE���ֱ��ʾ�������Ŷӡ�˽�� |
| parentId | string/null | ��֪ʶ�� ID��Ϊ�ձ�ʾ����֪ʶ�� |
| teamId | string/null | �Ŷ�֪ʶ�������Ŷ� ID������/˽��֪ʶ��Ϊ�� |
| teamName | string/null | �Ŷ�֪ʶ�������Ŷ����� |
| myRole | string | ��ǰ�û��ڸ�֪ʶ���е�Э����ɫ��READER/EDITOR/ADMIN |

Ȩ�޹���

- type=DEPARTMENT ��֪ʶ�����󶨹̶� teamId�������洴�����Ŷӱ仯��Ư�ơ�
- �û��ܿ�������֪ʶ�⡢�Լ������Ŷӵ��Ŷ�֪ʶ�⡢�Լ�����������֪ʶ�⡣
- �ɼ�ֻ�������Ķ����������༭��ɾ���������Ա��������֪ʶ���Ա��ɫ��
- �ĵ�Ȩ�޲���ͻ��֪ʶ��Ȩ�ޣ��û�������֪ʶ��ʱ����ʹ�ĵ��ǹ����ĵ�Ҳ���ܷ��ʡ�
- ��֪ʶ��ʹ�� parentId ������֯��ϵ�������������֪ʶ�⣻��Ա��Ȩ�޲��Զ��̳и�֪ʶ�⡣
