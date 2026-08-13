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
- `GET /api/practice/tags`
- `POST /api/practice/draw`
- `GET /api/problem-lists`
- `POST /api/problem-lists`
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
- `password` 可不传或传空字符串（表示不修改密码）。非空密码长度需 `>= 8`。
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

接口立即返回异步任务，不会让 HTTP 请求线程等待 Codeforces 限流队列：
```json
{
  "jobId": "8f9d...",
  "status": "PENDING",
  "message": "校验任务已进入队列",
  "errorCode": null,
  "result": null
}
```
- `GET /api/daily-problem/check-in/{jobId}` 查询任务；终态为 `SUCCEEDED` 或 `FAILED`。
- 相同用户和提交 ID 在 10 分钟内复用同一任务，任务结果在内存保留 1 小时。
- 任务在提交时固定训练日期，因此排队跨过零点也不会误校验下一日题目。

规则：
- 校验该提交是否属于当前用户且对应今日题（easy 或 hard slot）。
- 一次打卡只读取一次提交记录，再在本地匹配 easy/hard，避免重复调用 Codeforces。
- 仅 `verdict=OK` 记分；记分值为题目 `rating`。
- 同一用户同一天可多次打卡，取 max rating 为当日得分。
- 已被重抽（`is_redrawn=true`）的 slot 不计入。
- 打卡积分通过 per-user 同步锁保证并发安全。
- Codeforces 请求由全局优先队列统一节流，交互式打卡优先于后台资料刷新。

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
  "maxRating": 1600,
  "tags": ["dp", "greedy"]
}
```
- 多个标签采用 AND 语义，题目必须同时包含全部所选标签；最多选择 5 个。
- `GET /api/practice/tags` 返回当前本地题库中已去重、排序的可用标签。

自主练习仅记录抽题历史，不进行提交校验；提交校验只保留在每日一题。

### 4.5 管理员重生成今日题
- `POST /api/admin/daily-problem/regenerate`

### 4.6 管理员单题重抽
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

## 8. 个人与共享题单接口

- `GET /api/problem-lists`：返回当前用户自己的题单，以及管理员发布的全站共享题单。
- `GET /api/problem-lists/{id}`：读取自己的题单或共享题单详情。
- `POST /api/problem-lists`：创建一级题单。
- `PATCH /api/problem-lists/{id}`：题单创建者修改名称、简介和共享状态。
- `DELETE /api/problem-lists/{id}`：题单创建者删除题单及其全部题目。
- `POST /api/problem-lists/{id}/items`：向自己的题单添加题目。
- `PATCH /api/problem-lists/{id}/items/{itemId}`：修改题目快照。
- `DELETE /api/problem-lists/{id}/items/{itemId}`：从题单移除题目。

创建/修改题单请求体：
```json
{
  "name": "区间 DP",
  "description": "经典模型与易错题",
  "shared": false
}
```

添加题目请求体：
```json
{
  "link": "https://codeforces.com/problemset/problem/607/B",
  "title": "",
  "problemKey": "",
  "rating": null,
  "tags": "",
  "note": "重点理解状态转移"
}
```

- 题单只有一级，不允许嵌套子题单。
- 普通用户的题单仅自己可见；`ADMIN`、`SUPER_ADMIN` 可将自己创建的题单设为 `shared=true`，供所有登录成员只读查看。
- 只有创建者可以修改题单和其中的题目，包括管理员也不能修改他人的共享题单。
- 每日题、自主练习结果及其历史记录复用 `POST /api/problem-lists/{id}/items` 快捷加入题单；前端也支持在弹窗内新建个人题单后立即加入。
- 标准 Codeforces 链接会自动识别题号，并尝试从本地 `cf_problem` 补全标题、Rating 与标签；其他 OJ 链接需要填写标题。

## 9. 角色管理接口

### 9.1 查询角色列表
- `GET /api/roles`
- 权限：管理员及以上。
- 返回基于枚举的角色列表：`[{name, code, description}]`。

### 9.2 修改用户角色（超管理员）
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

### 9.3 现役队员训练看板
- `GET /api/admin/training-dashboard`
- 权限：`ADMIN`、`SUPER_ADMIN`。
- 返回内容：现役人数、今日完成数、近 7 天活跃人数和打卡率、7 日趋势，以及每位现役队员的每日题、自主练习、积分和本地 Codeforces 快照摘要。
- 该接口只读取数据库中的本地快照，不会在管理员打开看板时逐个请求 Codeforces。

### 9.4 导出现役队员训练数据
- `POST /api/admin/training-dashboard/export`
- 权限：`ADMIN`、`SUPER_ADMIN`。
- 请求体：
```json
{
  "range": "WEEK",
  "includeDaily": true,
  "includeCodeforcesContests": true,
  "includeAtCoderContests": true
}
```
- `range` 可选 `WEEK`（最近 7 天）、`MONTH`（最近 30 天）、`ALL`（全部本地历史）。
- 三个 `include*` 字段至少选择一个。接口返回 `.xlsx` 文件，其中始终包含“成员汇总”，并按选择增加“每日一题”“CF Rating比赛”“AtCoder ABC”工作表。
- CF 比赛明细读取本地完整 Rating 历史；首次导出发现旧账号尚无历史时，会按成员自动调用 `user.rating` 补齐。成员在个人页刷新 Codeforces 资料时也会同步更新完整比赛历史。

### 9.5 AtCoder 账号绑定
- `POST /api/users/{id}/atcoder-binding/start`：本人提交 `{ "handle": "AtCoder用户名" }`，返回验证码和 10 分钟有效期。
- 用户将验证码填写到 AtCoder 个人设置的 `Affiliation` 并保存。
- `POST /api/users/{id}/atcoder-binding/finish`：本人完成验证并绑定；同一 AtCoder Handle 不能被多个站内账号绑定。

### 9.6 AtCoder ABC 管理看板
- `GET /api/admin/atcoder-abc?contestId=abc460`：查看指定或最近一场 ABC 的现役成员完成情况。
- `POST /api/admin/atcoder-abc/refresh?contestId=abc460`：立即重新同步比赛结果。
- `PATCH /api/admin/atcoder-abc/setting`：设置最低赛时 AC 数和赛后最终判定等待小时数。
- `PATCH /api/admin/atcoder-abc/{contestId}/members/{userId}/exemption`：设置或取消单场豁免；设置豁免时必须填写原因。
- 权限：`ADMIN`、`SUPER_ADMIN`。

判定状态：`UPCOMING`（待比赛）、`PENDING`（等待同步）、`COMPLETED`（参赛且 AC 达标）、`PARTICIPATED`（参赛但 AC 未达标）、`ABSENT`（等待期结束后仍无参赛历史）、`UNBOUND`（未绑定）、`EXEMPT`（已豁免）、`DATA_ERROR`（数据源异常，自动重试）。

## 10. 常见业务错误码
- `400` 参数错误、提交不匹配题目、文件格式不支持等
- `401` 未登录、token 无效或过期
- `403` 权限不足（非管理员、非本人）
- `404` 资源不存在
- `409` 今日已打卡
- `500` 服务端异常
- `503` Codeforces 拉题失败
