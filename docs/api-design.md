# 接口设计文档（前后端对接）

## 1. 基础信息
- 后端地址：`http://localhost:8080`
- API 前缀：`/api`
- 鉴权请求头：
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
- 业务是否成功请以 `code === 200` 判断。
- 当前项目大多数错误会返回 `HTTP 200 + code/message`，少量场景可能返回 `401`。

## 2. 鉴权分级

免鉴权接口：
- `GET /api/health`
- `POST /api/users/register`
- `POST /api/auth/login`

登录鉴权接口：
- `POST /api/auth/logout`
- `POST /api/auth/refresh`
- `GET /api/users`
- `GET /api/users/{id}`
- `PATCH /api/users/me`
- `GET /api/daily-problem/today`
- `POST /api/daily-problem/check-in`
- `GET /api/daily-problem/history`
- `POST /api/practice/draw`
- `POST /api/practice/check`

管理员接口（需 ADMIN）：
- `POST /api/admin/users`
- `POST /api/admin/daily-problem/regenerate`

## 3. 用户接口

### 3.1 注册
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
- `username` 必须是有效 Codeforces handle（后端调用 `user.info` 校验）。

### 3.2 登录
- `POST /api/auth/login`

返回字段包含：
- 用户信息（`id/username/email/role/uid/codeforcesRating/maxRating/online/lastOnlineTimeSeconds/avatarUrl`）
- `accessToken`
- `refreshToken`

### 3.3 刷新令牌
- `POST /api/auth/refresh`
- Header：`X-Refresh-Token`

### 3.4 登出
- `POST /api/auth/logout`
- Header：`Authorization` + `X-Refresh-Token`

### 3.5 修改个人信息
- `PATCH /api/users/me`
- `Content-Type: application/json`
- 仅当前登录用户可修改自己

请求体：
```json
{
  "email": "new_mail@example.com",
  "password": "new_password_123"
}
```

说明：
- `password` 可不传或传空字符串（表示不修改密码）。
- 非空密码长度需 `>= 6`。

### 3.6 上传头像
- `Content-Type: multipart/form-data`
- 表单字段：`file`

说明：
- 仅允许图片类型，支持：`png/jpg/jpeg/gif/webp`
- 文件大小上限：`2MB`
- 公开访问路径：`/uploads/avatars/{fileName}`

## 4. 每日一题与练习题接口

### 4.1 获取今日题（全员同题）
- `GET /api/daily-problem/today`

说明：
- 如果当天题目不存在，后端会立即自动生成一题。

### 4.2 每日题打卡（计分）
- `POST /api/daily-problem/check-in`
- 请求体：
```json
{
  "submissionId": 123456789
}
```

规则：
- 校验该提交是否属于当前用户且对应今日题。
- 仅 `verdict=OK` 记分（当前为 `+1`）。
- 同一用户同一天只能打卡一次。

### 4.3 每日题历史
- `GET /api/daily-problem/history?limit=14`

### 4.4 自主抽题（不计分）
- `POST /api/practice/draw`
- 请求体（可选）：
```json
{
  "minRating": 1200,
  "maxRating": 1600
}
```

### 4.5 练习题校验（不计分）
- `POST /api/practice/check`
- 请求体：
```json
{
  "drawId": 1,
  "submissionId": 123456789
}
```

### 4.6 管理员重生成今日题
- `POST /api/admin/daily-problem/regenerate`

## 5. 常见业务错误码
- `400` 参数错误、提交不匹配题目、文件格式不支持等
- `401` 未登录、token 无效或过期
- `403` 非管理员调用管理员接口
- `404` 资源不存在
- `409` 今日已打卡
- `500` 服务端异常
- `503` Codeforces 拉题失败
