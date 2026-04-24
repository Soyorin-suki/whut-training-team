# WHUT Training Team

标准 Java Web 前后端分离骨架项目（MVC）。

- 后端：Spring Boot + Maven + SQLite（当前示例仓储为内存实现）
- 前端：React + Vite
- 认证：Access Token 鉴权 + Refresh Token 刷新
- 用户分级：普通用户 `USER`、管理员 `ADMIN`

## 项目结构

```text
whut-training-team
├── backend
│   ├── src/main/java/com/whut/training
│   │   ├── common                 # 统一返回结构
│   │   ├── config                 # WebConfig、初始化数据
│   │   ├── controller             # 控制层（user/auth/admin）
│   │   ├── domain
│   │   │   ├── dto                # 请求/响应 DTO
│   │   │   ├── entity             # 实体
│   │   │   └── enums              # 枚举（UserRole）
│   │   ├── exception              # 全局异常处理
│   │   ├── interceptor            # 认证拦截器
│   │   ├── repository             # 数据访问层
│   │   ├── service                # 业务层
│   │   └── utils                  # 工具类
│   └── src/main/resources/application.yml
├── frontend
│   └── src
│       ├── api                    # 接口请求封装
│       └── views                  # 页面
└── docs
    ├── api-design.md              # 前后端对接接口文档
    └── db-design.md               # 库表设计
```

## 文件分级说明

### 1) 按职责分级

- `controller`：仅处理参数、鉴权入口、调用服务
- `service`：核心业务逻辑与规则
- `repository`：数据读写抽象
- `domain`：业务模型（实体、DTO、枚举）

### 2) 按权限分级

- 公共接口：`/api/health`、`/api/users/register`、`/api/auth/login`
- 登录用户接口（需 Access Token）：`/api/users/**`、`/api/admin/**`
- 刷新/登出接口（需 Refresh Token）：`/api/auth/refresh`、`/api/auth/logout`

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

## 默认管理员账号

- `username`: `admin`
- `password`: `admin123`

系统启动时会自动初始化该管理员（若不存在）。

## 接口文档

- [前后端对接接口设计](docs/api-design.md)
- [库表设计](docs/db-design.md)
