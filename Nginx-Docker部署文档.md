# Wiki 系统 Nginx Docker 部署文档

## 1. 部署目标

本部署方案将 Wiki 前端构建为静态资源，并运行在 Nginx Docker 容器中；Nginx 同时负责将 `/api` 与 `/ws` 请求反向代理到 Spring Boot 后端。

推荐使用 `docker-compose.yml` 一键启动完整环境：

- `wiki-nginx`：Nginx 静态资源服务与反向代理，对外访问入口。
- `wiki-backend`：Spring Boot 后端服务。
- `wiki-mysql`：MySQL 8 数据库。
- `wiki-redis`：Redis 缓存与协作编辑锁。

访问入口：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| Wiki 前端 | `http://localhost:8088` | Nginx 容器对外入口 |
| 后端健康检查 | `http://localhost:18080/actuator/health` | 直接访问后端容器映射端口 |
| Nginx 健康检查 | `http://localhost:8088/healthz` | Nginx 容器存活检查 |
| MySQL 调试端口 | `localhost:3307` | 宿主机连接容器内 MySQL |

默认管理员账号：

| 用户名 | 密码 |
| --- | --- |
| `admin` | `Admin@123456` |


## 2. 前置要求

本机需要安装并启动：

- Docker Desktop 或 Docker Engine
- Docker Compose

验证命令：

```powershell
docker --version
docker compose version
docker ps
```

## 3. 部署文件说明

| 文件 | 作用 |
| --- | --- |
| `Dockerfile` | 构建前端并生成 Nginx 镜像 |
| `nginx/templates/default.conf.template` | Nginx 站点配置模板，支持容器环境变量 |
| `backend/Dockerfile` | 构建 Spring Boot 后端镜像 |
| `docker-compose.yml` | 编排 Nginx、后端、MySQL、Redis |
| `.dockerignore`、`backend/.dockerignore` | 减少 Docker 构建上下文，避免打包日志和依赖目录 |

## 4. 一键完整部署

在项目根目录执行：

```powershell
cd D:\code\Wiki
docker compose up -d --build
```

首次构建会下载 Node、Maven、JDK、Nginx、MySQL、Redis 镜像，并安装前后端依赖，耗时会比较久。

查看容器状态：

```powershell
docker compose ps
```

正常情况下应看到：

- `wiki-mysql` 为 `healthy`
- `wiki-redis` 为 `healthy`
- `wiki-backend` 为 `healthy`
- `wiki-nginx` 为 `running` 或 `healthy`

访问系统：

```text
http://localhost:8088
```

## 5. 部署验证

验证 Nginx 容器：

```powershell
curl http://localhost:8088/healthz
```

预期返回：

```text
ok
```

验证后端健康状态：

```powershell
curl http://localhost:18080/actuator/health
```

预期返回包含：

```json
{"status":"UP"}
```

验证前端静态资源：

```powershell
curl -I http://localhost:8088
```

预期返回 `HTTP/1.1 200 OK`。

浏览器登录验证：

1. 打开 `http://localhost:8088`
2. 使用 `admin / Admin@123456` 登录
3. 创建或查看知识库
4. 进入编辑页，确认页面不会出现接口 404 或 WebSocket 连接失败

## 6. Nginx 反向代理说明

Nginx 容器内的核心配置：

```nginx
location /api/ {
    proxy_pass http://${BACKEND_HOST}:${BACKEND_PORT};
}

location /ws/ {
    proxy_pass http://${BACKEND_HOST}:${BACKEND_PORT};
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
}

location / {
    try_files $uri $uri/ /index.html;
}
```

说明：

- 前端构建时 `VITE_API_BASE_URL=/api`，浏览器请求会走同域 `/api`。
- `/api/` 代理到后端容器 `wiki-backend:8080`。
- `/ws/` 支持协作编辑 WebSocket。
- `try_files ... /index.html` 支持 Vue Router history 路由，刷新 `/admin`、`/kbs/...` 等页面不会 404。
- 静态资源启用 30 天缓存，提高访问速度。

## 7. 单独运行 Nginx 容器

如果后端已经在宿主机启动，例如 `http://localhost:8080`，也可以只运行 Nginx 容器：

```powershell
docker build -t wiki-nginx:local .
docker run -d --name wiki-nginx `
  -p 8088:80 `
  -e BACKEND_HOST=host.docker.internal `
  -e BACKEND_PORT=8080 `
  wiki-nginx:local
```

Linux 环境如无法解析 `host.docker.internal`，增加：

```powershell
--add-host=host.docker.internal:host-gateway
```

停止并删除单独运行的 Nginx 容器：

```powershell
docker rm -f wiki-nginx
```

## 8. 常用运维命令

查看日志：

```powershell
docker compose logs -f nginx
docker compose logs -f backend
docker compose logs -f mysql
docker compose logs -f redis
```

重启服务：

```powershell
docker compose restart nginx
docker compose restart backend
```

停止服务但保留数据卷：

```powershell
docker compose down
```

停止服务并删除数据库、Redis、上传文件等数据卷：

```powershell
docker compose down -v
```

重新构建镜像：

```powershell
docker compose build --no-cache
docker compose up -d
```

进入容器排查：

```powershell
docker exec -it wiki-nginx sh
docker exec -it wiki-backend sh
docker exec -it wiki-mysql mysql -uroot -proot123456
```

## 9. 关键环境变量

后端环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://mysql:3306/wiki...` | MySQL 连接地址 |
| `DB_USERNAME` | `wiki` | 数据库用户 |
| `DB_PASSWORD` | `wiki123456` | 数据库密码 |
| `REDIS_HOST` | `redis` | Redis 主机名 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `JWT_SECRET` | 示例值 | JWT 签名密钥，生产必须修改 |
| `ADMIN_USERNAME` | `admin` | 初始化管理员用户名 |
| `ADMIN_PASSWORD` | `Admin@123456` | 初始化管理员密码 |
| `LOCAL_STORAGE_DIR` | `/app/storage/docs` | 文档文件存储目录 |
| `AVATAR_STORAGE_DIR` | `/app/storage/avatars` | 头像存储目录 |
| `DOC_IMAGE_STORAGE_DIR` | `/app/storage/doc-images` | 文档图片存储目录 |

Nginx 环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BACKEND_HOST` | `backend` | 后端服务主机名 |
| `BACKEND_PORT` | `8080` | 后端服务端口 |

## 10. 数据持久化

Compose 已配置三个 Docker Volume：

| Volume | 用途 |
| --- | --- |
| `wiki-nginx-deploy_wiki-mysql-data` | MySQL 数据 |
| `wiki-nginx-deploy_wiki-redis-data` | Redis AOF 数据 |
| `wiki-nginx-deploy_wiki-backend-storage` | 文档、头像、文档图片 |

只执行 `docker compose down` 不会删除这些数据；执行 `docker compose down -v` 会删除所有数据卷。

## 11. 常见问题

### 11.1 访问 `http://localhost:8088` 失败

检查 Nginx 容器是否启动：

```powershell
docker compose ps nginx
docker compose logs nginx
```

如果 `8088` 端口被占用，修改 `docker-compose.yml` 中：

```yaml
ports:
  - "8088:80"
```

例如改为：

```yaml
ports:
  - "8090:80"
```

### 11.2 页面能打开，但接口返回 502

说明 Nginx 已启动，但后端不可用。检查：

```powershell
docker compose ps backend
docker compose logs backend
curl http://localhost:18080/actuator/health
```

常见原因：

- MySQL 未健康启动
- Redis 未健康启动
- 后端数据库连接参数错误
- 后端启动时间较长，稍等后重试

### 11.3 后端一直 unhealthy

查看后端日志：

```powershell
docker compose logs -f backend
```

重点检查 MySQL、Redis、端口和配置项。首次启动数据库初始化可能需要几十秒。

### 11.4 路由刷新后 404

本方案已在 Nginx 中配置：

```nginx
try_files $uri $uri/ /index.html;
```

如果仍出现 404，确认运行的是最新镜像：

```powershell
docker compose build --no-cache nginx
docker compose up -d nginx
```

### 11.5 WebSocket 协作编辑失败

检查 Nginx `/ws/` 代理和后端日志：

```powershell
docker compose logs nginx
docker compose logs backend
```

浏览器开发者工具中应看到连接地址类似：

```text
ws://localhost:8088/ws/collab?token=...
```

## 12. 生产环境建议

正式部署时建议调整：

- 修改 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`JWT_SECRET`、`ADMIN_PASSWORD`。
- 不要向公网暴露 MySQL 和后端调试端口，可删除 `3307:3306` 与 `18080:8080` 端口映射。
- 在 Nginx 前增加 HTTPS 证书，或由云负载均衡统一终止 TLS。
- 定期备份 MySQL Volume 与 `/app/storage` 数据。
- 将 `DEMO_DATA_ENABLED` 改为 `false`，避免生产环境生成演示数据。
- 使用更严格的防火墙规则，只开放前端入口端口。

## 13. 验收清单

部署完成后，建议按以下清单验收：

- `docker compose ps` 中四个容器均正常运行。
- `http://localhost:8088/healthz` 返回 `ok`。
- `http://localhost:18080/actuator/health` 返回 `UP`。
- 浏览器可打开 `http://localhost:8088`。
- 管理员账号可登录。
- 知识库列表、创建、查看功能正常。
- 文档编辑页 WebSocket 无连接错误。
- 刷新任意前端路由不出现 404。
