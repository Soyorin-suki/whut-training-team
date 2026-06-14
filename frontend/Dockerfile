# Stage 1: 构建前后端静态资源
FROM node:20-alpine AS builder
WORKDIR /app

# 利用缓存层
COPY package.json package-lock.json* ./
RUN npm install

# 复制源码并构建
COPY . .
RUN npm run build

# Stage 2: 使用 Nginx 提供服务并反向代理后端
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从 builder 阶段复制构建产物到 Nginx 默认目录
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
