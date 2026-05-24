# Todo List

> 历史开发记录，按功能模块组织。`[x]` = 已完成。

---

## 用户系统

- [x] **注册**：以 Codeforces handle 为用户名，后端校验 CF 用户是否存在，同步 avatarUrl
- [x] **登录 / Token 刷新 / 注销**：Access Token（30min）+ Refresh Token（7d）双重令牌，401 自动静默刷新
- [x] **个人资料修改**：昵称（displayName）、头像（avatarUrl）、个人简介（bio）、邮箱、密码
- [x] **三级权限**：`USER` / `ADMIN` / `SUPER_ADMIN`
- [x] **超管理员角色变更**：任意修改用户角色，写入 `role_change_log` 审计表
- [x] **Auth 缓存失效处理**：token 刷新失败时自动清除缓存并跳转登录页

## 每日一题

- [x] **双题位（easy + hard）**：按 rating 阈值（配置 `app.daily.ratingThreshold`，默认 1700）分割
- [x] **Codeforces 题库同步**：定时拉取 `problemset.problems` 存入 `cf_problem`，首次启动兜底同步
- [x] **定时自动生成**：每日按 cron 自动出题，无题时接口即时补生成
- [x] **打卡校验**：提交 Codeforces submissionId，后端校验匹配题目且 verdict=OK，积分 = 题目 rating
- [x] **管理员重生成 / 重抽**：regenerate 重出全部，redraw 重抽单题（旧题标记 is_redrawn=1）
- [x] **刷新/重抽后状态展示**：已打卡的旧题保留在上方并标记，未打卡的归入历史显示"已被刷新"
- [x] **历史查询**：按日期范围查询所有 slot 及个人打卡状态

## 自主练习

- [x] **随机抽题**：按 rating 范围随机从 `cf_problem` 抽取，不计分
- [x] **提交校验**：同打卡校验流程，verdict=OK 即通过
- [x] **练习历史**：抽题记录落库 `user_practice_draw`，前端展示 + 支持删除

## 排行榜

- [x] **总积分排名**：按 `users.total_points` 降序，平局按最后打卡时间排序，支持分页

## 首页聚合

- [x] **活跃用户**：过去 7 日内至少打卡一次的去重用户数
- [x] **排行榜 Top N**：预览前 N 名
- [x] **今日题目**：展示当日 easy + hard 两题（过滤已重抽题）
- [x] **今日推题**：当天已发布的推题
- [x] **打卡统计**：今日打卡用户数 + 今日提交总数
- [x] **打卡热力图**：GitHub 风格 365 天贡献图，按得分 colorLevel 0-4 着色

## 推题系统

- [x] **用户提交推题**：提交 title + link，状态 PENDING
- [x] **管理员审核**：通过 / 拒绝（状态 APPROVED / REJECTED）
- [x] **推题池**：已通过未推送的推题列表，管理员可删除、提升优先级（sort_order）
- [x] **每日定时推送**：按 sort_order 从池中 pop 一条，记录到 daily_push
- [x] **推题解答**：用户提交解答链接 + 描述
- [x] **推题历史**：按日期查看已推送的推题记录（含推送日期）

## 管理员功能

- [x] **创建用户**
- [x] **每日题管理**：重生成全部 + 按 slot 重抽单题
- [x] **推题管理**：审核（通过/拒绝）、推题池管理（删除/提升）、查看提交
- [x] **角色管理**（SUPER_ADMIN）：修改任意用户角色

---

## 待完成

- [ ] 定时任务与 CF API 的重试策略与告警通知
- [ ] 连续打卡天数统计
- [ ] 用户搜索功能
- [ ] 与 QQ Bot 数据库对接
- [ ] 自动化测试与验收用例
