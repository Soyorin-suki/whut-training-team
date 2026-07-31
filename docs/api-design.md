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
- `PATCH /api/users/{id}`
- `GET /api/users/{user_id}/daily-heatmap`
- `GET /api/daily-problem/today`
- `POST /api/daily-problem/check-in`
- `GET /api/daily-problem/history`
- `POST /api/practice/draw`
- `POST /api/practice/check`
- `POST /api/push`
- `GET /api/push`
- `POST /api/push/{id}/submit`
- `GET /api/push/{id}/submissions`

管理员接口（需 ADMIN）：
- `POST /api/admin/users`
- `POST /api/admin/daily-problem/regenerate`
- `POST /api/admin/daily-problem/redraw`
- `POST /api/admin/push/{id}/approve`
- `POST /api/admin/push/{id}/reject`
- `POST /api/admin/push/{id}/promote`
- `GET /api/roles`

超管理员接口（需 SUPER_ADMIN）：
- `PUT /api/admin/users/{id}/role`

## 3. 用户接口

### 3.1 注册
- `POST /api/users/register`
- `Content-Type: application/json`

请求示例：
```json
{
  "username": "alice-account",
  "displayName": "Alice",
  "password": "123456"
}
```

说明：
- `username` 是唯一登录账号，`displayName` 是对外展示的用户名。
- 注册成功并登录后，通过独立绑定接口验证 Codeforces 账号所有权。

### 3.2 登录
- `POST /api/auth/login`

返回字段包含：
- 用户信息（`id/username/email/role/uid/codeforcesRating/maxRating/online/lastOnlineTimeSeconds/avatarUrl/displayName/bio/totalPoints`）
- `accessToken`
- `refreshToken`

### 3.3 刷新令牌
- `POST /api/auth/refresh`
- Header：`X-Refresh-Token`

### 3.4 登出
- `POST /api/auth/logout`
- Header：`Authorization` + `X-Refresh-Token`

### 3.5 获取用户信息
- `GET /api/users/{id}`
- 权限：仅本用户或管理员可查看。普通用户查看他人信息返回 403。

### 3.6 修改个人信息
- `PATCH /api/users/{id}`
- `Content-Type: application/json`
- 权限：仅本用户或管理员可修改。

请求体：
```json
{
  "username": "new_username",
  "email": "new@example.com",
  "password": "newpass123",
  "displayName": "Alice",
  "avatar": "https://...",
  "bio": "Hello world"
}
```

说明：
- `password` 可不传或传空字符串（表示不修改密码）。非空密码长度需 `>= 6`。
- `displayName` / `avatar` / `bio` 新增字段，可局部更新。
- 修改 `username` 只修改站内登录名，不影响已绑定的 Codeforces Handle。

### 3.7 用户每日提交热图
- `GET /api/users/{user_id}/daily-heatmap?days=180`
- 返回：`[{date, score, colorLevel}]`
- `colorLevel` 为 0-4，由得分相对强度自动计算。
- 权限：本用户或管理员。

### 3.8 开始绑定 Codeforces
- `POST /api/users/{id}/codeforces-binding/start`
- 权限：只能操作当前登录用户。

请求体：
```json
{
  "handle": "tourist"
}
```

服务端记录验证开始时间。用户须在 2 分钟内使用该 Handle 向
`https://codeforces.com/contest/1/problem/A` 提交一份产生
`COMPILATION_ERROR` 的代码。

### 3.9 完成 Codeforces 绑定
- `POST /api/users/{id}/codeforces-binding/finish`
- 权限：只能操作当前登录用户。

服务端读取该 Handle 最近的提交；若发现验证开始后产生的 1A 编译错误提交，
则保存绑定关系并同步 Codeforces rating、头像等资料。

## 4. 每日一题与练习题接口

### 4.1 获取今日题（多题位 easy/hard）
- `GET /api/daily-problem/today`
- 返回示例：
```json
{
  "code": 200,
  "data": {
    "problems": [
      {
        "type": "EASY",
        "date": "2026-05-23",
        "problemKey": "1234-A",
        "contestId": 1234,
        "problemIndex": "A",
        "name": "Problem Name",
        "rating": 1500,
        "tags": "dp,greedy",
        "sourceUrl": "https://codeforces.com/..."
      },
      {
        "type": "HARD",
        "date": "2026-05-23",
        "problemKey": "5678-D",
        "contestId": 5678,
        "problemIndex": "D",
        "name": "Hard Problem",
        "rating": 1900,
        "tags": "graph",
        "sourceUrl": "https://codeforces.com/..."
      }
    ],
    "checkedIn": false,
    "score": 0
  }
}
```
- `checkedIn` 表示用户当日是否已打卡（任意 slot 通过即可）。
- `score` 为当日当前最高得分（取 max rating）。

### 4.2 每日题打卡（计分）
- `POST /api/daily-problem/check-in`
- 请求体：
```json
{
  "submissionId": 123456789
}
```

规则：
- 校验该提交是否属于当前用户且对应今日题（easy 或 hard slot）。
- 仅 `verdict=OK` 记分；记分值为题目 `rating`。
- 同一用户同一天可多次打卡，取 max rating 为当日得分。
- 已被重抽（`is_redrawn=true`）的 slot 不计入。
- 打卡积分通过 per-user 同步锁保证并发安全。

### 4.3 每日题历史
- `GET /api/daily-problem/history?days=0`
- `days=0` 或不传表示查询全部历史记录。
- 返回示例：
```json
[
  {
    "date": "2026-05-23",
    "slot": "easy",
    "problemKey": "1234-A",
    "name": "Problem Name",
    "rating": 1500,
    "sourceUrl": "https://...",
    "isRedrawn": false,
    "checkedIn": true,
    "submissionId": 123456789,
    "verdict": "OK",
    "score": 1500
  }
]
```

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

### 4.7 管理员单题重抽
- `POST /api/admin/daily-problem/redraw?slot=easy|hard&confirm=false`
- `slot` 必填（easy/hard），`confirm` 保留扩展。
- 旧题标记 `is_redrawn=true`，新题插入；已打卡的旧题记录保留且计分有效。

## 5. 排行榜接口

- `GET /api/leaderboard?limit=10&page=1&type=total`
- 返回：
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "userId": 1,
        "username": "alice",
        "totalPoints": 3500,
        "lastCheckinAt": "2026-05-23T10:30:00"
      }
    ],
    "total": 100,
    "page": 1,
    "limit": 10
  }
}
```
- `limit` 默认 10，最大 100。
- `page` 默认 1（1-based）。
- 排序规则：`total_points DESC, last_checkin_at DESC`。

## 6. 首页聚合接口

- `GET /api/home?top=10`
- 返回示例：
```json
{
  "totalUsers": 120,
  "topUsers": [ ... ],
  "todayProblem": [ ... ],
  "todayPushProblem": { ... },
  "dailySubmissionSummary": {
    "todaySubmissions": 45,
    "todayCheckedInUsers": 30
  }
}
```
- `todayProblem` 为今日多题位（easy/hard）列表。
- `todayPushProblem` 为当日推送题目（无推送时为 null）。
- `dailySubmissionSummary` 包含当日总提交数和打卡用户数。

## 7. 推题系统接口

### 7.1 提交推题
- `POST /api/push`
- 请求体：
```json
{
  "title": "Interesting Problem",
  "link": "https://codeforces.com/...",
  "description": "A nice dp problem"
}
```
- `title` 和 `link` 必填，`description` 可选。

### 7.2 查看推题池
- `GET /api/push`
- 普通用户仅看到已审批（APPROVED）的题目，管理员看到全部。

### 7.3 提交推题解答
- `POST /api/push/{id}/submit`
- 请求体：
```json
{
  "submissionLink": "https://codeforces.com/...",
  "resultDescription": "Solved using dp"
}
```
- `submissionLink` 必填。

### 7.4 查看推题提交
- `GET /api/push/{id}/submissions`
- 管理员查看该题所有提交，普通用户仅看到自己的提交。

### 7.5 管理员审批
- `POST /api/admin/push/{id}/approve`

### 7.6 管理员拒绝
- `POST /api/admin/push/{id}/reject`

### 7.7 管理员提升
- `POST /api/admin/push/{id}/promote`
- 将该题提升至推题队列最前。

## 8. 角色管理接口

### 8.1 查询角色列表
- `GET /api/roles`
- 权限：管理员及以上。
- 返回基于枚举的角色列表：`[{name, code, description}]`。

### 8.2 修改用户角色（超管理员）
- `PUT /api/admin/users/{id}/role`
- 权限：仅 SUPER_ADMIN。
- 请求体：
```json
{
  "role": "ADMIN"
}
```
- 有效值：`USER`, `ADMIN`, `SUPER_ADMIN`。
- 操作记录写入 `role_change_log` 表。

## 9. 常见业务错误码
- `400` 参数错误、提交不匹配题目、文件格式不支持等
- `401` 未登录、token 无效或过期
- `403` 权限不足（非管理员、非本人）
- `404` 资源不存在
- `409` 今日已打卡
- `500` 服务端异常
- `503` Codeforces 拉题失败
