# WHUT Training Team

标准 Java Web 前后端分离骨架项目（MVC）。

- 后端：Spring Boot + Maven + SQLite（当前示例仓储为内存实现）
- 前端：React + Vite
- 认证：双令牌（Access Token + Refresh Token）+ 拦截器校验
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
    └── api-design.md              # 前后端对接接口文档
```

## 文件分级说明

### 1) 按职责分级

- `controller`：仅处理参数、鉴权入口、调用服务
- `service`：核心业务逻辑与规则
- `repository`：数据读写抽象
- `domain`：业务模型（实体、DTO、枚举）

### 2) 按权限分级

- 公共接口：`/api/health`、`/api/users/register`、`/api/auth/login`
- 登录用户接口（需双令牌）：`/api/users/**`、`/api/auth/logout`
- 管理员接口（需双令牌 + ADMIN）：`/api/admin/**`

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

## 库表设计

当前代码示例使用内存仓储，但推荐 SQLite 物理模型如下：

### 1) `users` 用户表

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 用户主键 |
| username | TEXT | NOT NULL, UNIQUE, length <= 50 | 用户名 |
| email | TEXT | NOT NULL | 邮箱 |
| password | TEXT | NOT NULL | 密码（生产应存哈希） |
| role | TEXT | NOT NULL, CHECK(role in ('USER','ADMIN')) | 角色 |
| created_at | TEXT | NOT NULL | 创建时间 |

### 2) `auth_sessions` 会话表（双令牌）

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| access_token | TEXT | PRIMARY KEY | 访问令牌 |
| refresh_token | TEXT | UNIQUE, NOT NULL | 刷新令牌 |
| user_id | INTEGER | NOT NULL | 关联用户 |
| access_expired_at | TEXT | NOT NULL | Access 过期时间 |
| refresh_expired_at | TEXT | NOT NULL | Refresh 过期时间 |

### SQLite 建表 SQL（建议）

```sql
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE CHECK (length(username) <= 50),
  email TEXT NOT NULL,
  password TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('USER', 'ADMIN')),
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS auth_sessions (
  access_token TEXT PRIMARY KEY,
  refresh_token TEXT NOT NULL UNIQUE,
  user_id INTEGER NOT NULL,
  access_expired_at TEXT NOT NULL,
  refresh_expired_at TEXT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_id ON auth_sessions(user_id);
```
