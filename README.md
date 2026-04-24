# WHUT Training Team

标准 Java Web 前后端分离骨架项目，采用 MVC 架构。  
后端：JDK 22 + Spring Boot + SQLite  
前端：React + Vite

## 项目结构

```text
whut-training-team
├── backend                      # Spring Boot 后端
│   ├── src/main/java/com/whut/training
│   │   ├── controller           # 控制层
│   │   ├── service              # 业务层
│   │   ├── repository           # 数据访问层
│   │   ├── domain               # 实体和 DTO
│   │   ├── common               # 通用返回对象
│   │   ├── config               # 配置
│   │   └── exception            # 全局异常处理
│   └── src/main/resources/application.yml
└── frontend                     # React 前端
    └── src
        ├── api                  # 接口请求封装
        └── views                # 页面
```

## 启动方式

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 示例接口

- `GET /api/health`：健康检查
- `GET /api/users`：查询用户列表
- `POST /api/users`：新增用户
