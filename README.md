# Wiki 协同知识库项目

> 技术栈：Spring Boot 3 + Spring Data JPA + MySQL 8 + Redis + WebSocket(OT) + Vue 3 + Vite

## 目录结构

- `backend`：后端服务
- `frontend`：前端工程（用户端 + 管理端）
- `docs`：需求对照与说明

## 完成情况（全部要求）

- 用户认证：注册、登录、JWT鉴权、普通用户/管理员角色区分
- 注册头像：支持注册时可选上传头像（也可手填头像URL）
- 注册验证码：邮箱/手机号六位验证码；未配置邮件或短信网关时自动测试模式回码
- 发送频控：同一IP/手机号/邮箱 1 分钟内最多请求 1 次
- 用户资料：昵称/头像/邮箱/手机号修改，邮箱手机号变更需验证码
- 唯一性约束：用户名、邮箱、手机号各自唯一
- 知识库：多知识库（公开/私有/团队）+ 独立目录树（无限层级）
- 成员协作：成员邀请与权限控制（只读/编辑/管理员）
- 文档管理：Markdown编辑、发布状态、文档树管理
- 版本历史：保存版本、查看版本、回滚任意版本
- 版本对比：双版本行级 diff（新增/删除/修改/未变）
- 全文检索：标题/内容 LIKE 搜索
- 文档权限：公开/团队/私有访问与编辑控制
- 协同编辑（OT）：
  - WebSocket 实时协作
  - Operational Transform 并发变换
  - 实时协作者光标/选区（头像/姓名）
  - 冲突自动合并与手动冲突处理提示
- 自动草稿：编辑页每30秒自动保存
- Redis缓存：最近更新、最多阅读、发布HTML缓存、`editing:docId`编辑锁
- 分享能力：分享链接访问（有效期控制）
- 回收站：`deleted_at`软删除、恢复、彻底删除二次确认、30天保留
- 异步清理：删除数据库记录后异步清理本地文件 + 云存储清理接口
- 操作日志：登录、删除、分享、权限变更等关键操作记录
- 管理后台：按时间/IP/用户/操作分页查看日志
- UI一致性：统一弹窗、提示、加载状态、浅色/暗黑主题（CSS变量）
- Snowflake ID：后端以字符串返回，前端不转 `Number`
- API文档：Swagger UI

## 运行步骤

## 1) 启动后端

前置环境：JDK17、Maven 3.9+、MySQL 8、Redis

建表脚本已提供（含外键）：

- `backend/sql/schema-mysql8-with-fk.sql`

可直接执行：

```bash
mysql -uroot -p < backend/sql/schema-mysql8-with-fk.sql
```

修改 `backend/src/main/resources/application.yml` 后启动：

```bash
cd backend
mvn spring-boot:run
```

如果终端里 `mvn` 不可用，或端口被旧后端进程占用，建议直接用一键脚本（Windows）：

```bat
cd backend
start-backend.cmd
```

MySQL 启动时也支持环境变量覆盖：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`（默认 `8080`）

默认管理员账号：

- 用户名：`admin`
- 密码：`Admin@123456`

## 2) 启动前端

前置环境：Node.js 18+

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:5173`

## 协作与存储配置

- WebSocket：`/ws/collab`
- 本地文档缓存目录：`wiki.local-storage-dir`（默认 `./storage/docs`）
- 短信网关：`wiki.sms-enabled`、`wiki.sms-gateway-url`、`wiki.sms-api-key`
- 云清理接口：`wiki.cloud-cleanup-enabled`、`wiki.cloud-cleanup-url`、`wiki.cloud-cleanup-api-key`

## API文档

后端启动后访问：

- `http://localhost:8080/swagger-ui.html`

## 注意事项

- 所有ID均以字符串返回，前端禁止转`Number`
- 如果短信/邮件未配置，系统会自动进入测试模式直接返回验证码
