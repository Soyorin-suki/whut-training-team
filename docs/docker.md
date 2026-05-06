# Docker

## 启动

```bash
docker compose up -d --build
```

## 代码改动后重建

只要仓库里的 Docker 文件仍然作为启动入口，改完代码后就重新执行：

```bash
docker compose up -d --build
```

## 常用检查

```bash
docker compose ps
```
