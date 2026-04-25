# 库表设计

当前后端使用 SQLite，核心表如下。

## 1) `users`（用户表）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | 用户主键 |
| username | TEXT | NOT NULL, UNIQUE | 用户名（Codeforces handle） |
| email | TEXT | NULL | 邮箱 |
| password | TEXT | NOT NULL | 密码（当前为明文，后续应改哈希） |
| role | TEXT | NULL | `USER` / `ADMIN` |
| uid | INTEGER | NULL | 从 Codeforces 头像 URL 解析出的 uid |
| codeforces_rating | INTEGER | NULL | CF 当前 rating |
| max_rating | INTEGER | NULL | CF 历史最高 rating |
| is_online | INTEGER | NULL | 在线状态（0/1） |
| last_online_time_seconds | INTEGER | NULL | 最近在线时间（Unix 秒） |
| avatar_url | TEXT | NULL | 头像地址 |

## 2) `cf_problem`（Codeforces 题库缓存）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | 主键 |
| problem_key | TEXT | NOT NULL, UNIQUE | 题目键（`contestId-index`） |
| contest_id | INTEGER | NOT NULL | 比赛 ID |
| problem_index | TEXT | NOT NULL | 题号（A/B/C...） |
| name | TEXT | NOT NULL | 题目标题 |
| rating | INTEGER | NULL | 题目难度 |
| tags | TEXT | NULL | 标签，逗号分隔 |
| is_interactive | INTEGER | NOT NULL DEFAULT 0 | 是否交互题 |
| source_contest_id | INTEGER | NULL | 来源 contest（非空通常需过滤） |
| solved_count | INTEGER | NULL | 通过人数 |
| source_url | TEXT | NOT NULL | 题目链接 |
| last_synced_at | TEXT | NOT NULL | 最后同步时间 |

## 3) `daily_problem`（每日题快照）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | 主键 |
| date | TEXT | NOT NULL, UNIQUE | 日期（YYYY-MM-DD） |
| problem_key | TEXT | NOT NULL | 题目键 |
| contest_id | INTEGER | NOT NULL | 比赛 ID |
| problem_index | TEXT | NOT NULL | 题号 |
| name | TEXT | NOT NULL | 标题 |
| rating | INTEGER | NULL | 难度 |
| tags | TEXT | NULL | 标签 |
| source_url | TEXT | NOT NULL | 链接 |
| generated_at | TEXT | NOT NULL | 生成时间 |
| generated_by | TEXT | NOT NULL | 生成来源（scheduler/api/admin） |

## 4) `user_daily_status`（每日题打卡）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | 主键 |
| user_id | INTEGER | NOT NULL | 用户 ID |
| date | TEXT | NOT NULL | 日期 |
| submission_id | INTEGER | NOT NULL | 提交 ID |
| verdict | TEXT | NOT NULL | 判题结果 |
| checked_at | TEXT | NOT NULL | 校验时间 |
| score | INTEGER | NOT NULL DEFAULT 1 | 得分 |

唯一约束：
- `(user_id, date)`，保证每日仅打卡一次。

## 5) `user_practice_draw`（自主抽题记录，不计分）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | 抽题记录 ID |
| user_id | INTEGER | NOT NULL | 用户 ID |
| draw_date | TEXT | NOT NULL | 抽题日期 |
| problem_key | TEXT | NOT NULL | 题目键 |
| contest_id | INTEGER | NOT NULL | 比赛 ID |
| problem_index | TEXT | NOT NULL | 题号 |
| name | TEXT | NOT NULL | 标题 |
| rating | INTEGER | NULL | 难度 |
| tags | TEXT | NULL | 标签 |
| source_url | TEXT | NOT NULL | 链接 |
| drawn_at | TEXT | NOT NULL | 抽题时间 |
| submission_id | INTEGER | NULL | 用户提交 ID |
| verdict | TEXT | NULL | 判题结果 |
| checked_at | TEXT | NULL | 校验时间 |
