# 接口设计文档（前后端对接）

## 1. 基础信息

- 后端地址：`http://localhost:8080`
- API 前缀：`/api`
- 认证机制：双令牌
  - `Authorization: Bearer <accessToken>`
  - `X-Refresh-Token: <refreshToken>`

统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

说明：
- 当前项目业务成功与否以 `code === 200` 判断。
- 目前大多数接口返回 HTTP 200，错误信息在 `code/message` 中体现。

## 2. 鉴权分级

免鉴权接口：
- `GET /api/health`
- `POST /api/users/register`
- `POST /api/auth/login`

登录鉴权接口：
- `POST /api/auth/logout`
- `GET /api/users`
- `GET /api/users/{id}`

管理员接口（需 ADMIN）：
- `POST /api/admin/users`

## 3. 数据模型

### 3.1 User

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| role | string | `USER` 或 `ADMIN` |
| uid | number | 从 Codeforces 头像链接解析出的 uid |
| codeforcesRating | number | Codeforces 当前 rating |
| maxRating | number | Codeforces 历史最高 rating |
| online | boolean | 是否在线 |
| lastOnlineTimeSeconds | number | 最近在线时间（Unix 秒） |
| avatarUrl | string | 头像 URL |

### 3.2 RegisterRequest

| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| username | string | 是 | 非空，长度 <= 50 |
| email | string | 否 | 合法邮箱 |
| password | string | 是 | 非空，长度 6-64 |
| codeforcesRating | number | 否 | 可选 |
| maxRating | number | 否 | 可选 |
| online | boolean | 否 | 可选 |
| lastOnlineTimeSeconds | number | 否 | 可选 |
| avatarUrl | string | 否 | 可选 |

### 3.3 LoginRequest

| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| username | string | 是 | 非空 |
| password | string | 是 | 非空 |

### 3.4 LoginResponse

登录成功后，返回用户除 `password` 外的所有属性（包含 `role`）以及双令牌。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| role | string | `USER` 或 `ADMIN` |
| uid | number | 从 Codeforces 头像链接解析出的 uid |
| codeforcesRating | number | Codeforces 当前 rating |
| maxRating | number | Codeforces 历史最高 rating |
| online | boolean | 是否在线 |
| lastOnlineTimeSeconds | number | 最近在线时间（Unix 秒） |
| avatarUrl | string | 头像 URL |
| accessToken | string | 访问令牌 |
| refreshToken | string | 刷新令牌 |

### 3.5 AdminCreateUserRequest

| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| username | string | 是 | 非空，长度 <= 50 |
| email | string | 是 | 非空，合法邮箱 |
| password | string | 是 | 非空，长度 6-64 |
| role | string | 是 | `USER` 或 `ADMIN` |

## 4. 接口详情

### 4.1 健康检查

- `GET /api/health`

成功示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "time": "2026-04-24T14:10:00"
  }
}
```

### 4.2 用户注册（默认 USER）

- `POST /api/users/register`
- `Content-Type: application/json`

请求示例：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456"
}
```

说明：
- `username` 必须是 Codeforces 上存在的 handle（通过 `user.info` 校验）。
- 若 `codeforcesRating/maxRating/online/lastOnlineTimeSeconds/avatarUrl` 未传，后端会根据 Codeforces 信息补全。
- 后端会从头像链接中解析 `uid` 并落库（例如 `https://userpic.codeforces.org/1592/avatar/xxx.jpg` 解析为 `1592`）。

### 4.3 登录（获取双令牌）

- `POST /api/auth/login`

请求示例：

```json
{
  "username": "alice",
  "password": "123456"
}
```

成功示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER",
    "uid": 1592,
    "codeforcesRating": 1800,
    "maxRating": 1900,
    "online": false,
    "lastOnlineTimeSeconds": 1713950836,
    "avatarUrl": "https://userpic.codeforces.org/1592/avatar/7cef566902732053.jpg",
    "accessToken": "xxx",
    "refreshToken": "yyy"
  }
}
```

### 4.4 登出（双令牌）

- `POST /api/auth/logout`
- 请求头：
  - `Authorization: Bearer <accessToken>`
  - `X-Refresh-Token: <refreshToken>`

### 4.5 查询用户列表

- `GET /api/users`
- 需要登录鉴权

### 4.6 查询用户详情

- `GET /api/users/{id}`
- 需要登录鉴权

### 4.7 管理员创建用户

- `POST /api/admin/users`
- 需要登录鉴权且当前用户为 `ADMIN`

请求示例：

```json
{
  "username": "bob",
  "email": "bob@example.com",
  "password": "123456",
  "role": "ADMIN"
}
```

## 5. 业务错误码

- `400`：参数错误、用户名重复、用户名不是有效 Codeforces handle
- `401`：令牌缺失/无效/过期，或登录凭证错误
- `403`：非管理员访问管理员接口
- `404`：资源不存在
- `500`：服务端异常

