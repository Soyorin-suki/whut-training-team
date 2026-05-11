# Docker

## 拓扑

- `frontend` 是唯一对外入口，宿主机暴露 `5173:80`
- `backend` 只加入内部网络 `app`，不再直接暴露宿主机端口
- 浏览器所有 `/api` 请求都由 `frontend/nginx.conf` 反代到 `backend:8080`

## 启动

```bash
docker compose up -d --build
```

## 常用检查

```bash
docker compose ps
```

## 说明

- 后端 SQLite 路径由 `SPRING_DATASOURCE_URL` 注入，默认写入 `/app/data/training.db`
- 前端构建参数 `VITE_API_BASE_URL` 默认留空，应用统一走相对路径 `/api/...`
- 持久卷 `backend-data` 和 `backend-logs` 会保留数据库与日志，重建容器不会清空历史数据
