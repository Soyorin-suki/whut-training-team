# 库表设计

当前后端使用 MySQL 8 + InnoDB，核心表如下。

> 数据库初始化由 `MySqlInitializer` 完成，Repository 使用 `JdbcTemplate` 和 MySQL 的 `ON DUPLICATE KEY UPDATE`。项目不迁移旧 SQLite 数据。

## 1) `users`（用户表）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 用户主键 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 站内登录账号 |
| email | TEXT | NULL | 邮箱 |
| password | TEXT | NOT NULL | 密码（当前为明文，后续应改哈希） |
| role | TEXT | NULL | `USER` / `ADMIN` / `SUPER_ADMIN` |
| uid | INTEGER | NULL | 从 Codeforces 头像 URL 解析出的 uid |
| codeforces_rating | INTEGER | NULL | CF 当前 rating |
| max_rating | INTEGER | NULL | CF 历史最高 rating |
| is_online | INTEGER | NULL | 在线状态（0/1） |
| last_online_time_seconds | INTEGER | NULL | 最近在线时间（Unix 秒） |
| avatar_url | TEXT | NULL | 头像地址 |
| total_points | INTEGER | NOT NULL DEFAULT 0 | 用户总积分（每日一题累计） |
| display_name | TEXT | NULL | 展示昵称 |
| bio | TEXT | NULL | 个人简介 |

## 2) `cf_problem`（Codeforces 题库缓存）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
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

## 3) `daily_problem`（每日题快照，兼容旧单题）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
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

## 4) `daily_problem_slot`（每日多题位，当前主表）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| date | TEXT | NOT NULL | 日期 |
| slot | TEXT | NOT NULL | 题位（easy/hard） |
| problem_key | TEXT | NOT NULL | 题目键 |
| contest_id | INTEGER | NOT NULL | 比赛 ID |
| problem_index | TEXT | NOT NULL | 题号 |
| name | TEXT | NOT NULL | 标题 |
| rating | INTEGER | NULL | 难度 |
| tags | TEXT | NULL | 标签 |
| source_url | TEXT | NOT NULL | 链接 |
| generated_at | TEXT | NOT NULL | 生成时间 |
| generated_by | TEXT | NOT NULL | 生成来源 |
| is_redrawn | INTEGER | NOT NULL DEFAULT 0 | 是否已被重抽 |

索引：
- `idx_daily_problem_slot_date` on `(date)`

## 5) `user_daily_status`（每日题打卡记录）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| user_id | INTEGER | NOT NULL | 用户 ID |
| date | TEXT | NOT NULL | 日期 |
| submission_id | INTEGER | NOT NULL | Codeforces 提交 ID |
| verdict | TEXT | NOT NULL | 判题结果 |
| checked_at | TEXT | NOT NULL | 校验时间 |
| score | INTEGER | NOT NULL DEFAULT 1 | 得分（由题目 rating 决定） |

唯一约束：`(user_id, date)`，保证每日仅一次打卡记录（可被更高分覆盖）。

## 6) `user_practice_draw`（自主抽题记录，不计分）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 抽题记录 ID |
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

注：自主练习提交校验已停止使用；`submission_id`、`verdict`、`checked_at` 仅保留用于兼容已有历史数据，新抽题不会再写入这些字段。

## 7) `auth_token_session`（认证会话）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| user_id | INTEGER | NOT NULL | 用户 ID |
| access_token | TEXT | NOT NULL, UNIQUE | 访问令牌 |
| refresh_token | TEXT | NOT NULL, UNIQUE | 刷新令牌 |
| access_expired_at_seconds | INTEGER | NOT NULL | access token 过期时间 |
| refresh_expired_at_seconds | INTEGER | NOT NULL | refresh token 过期时间 |
| created_at_seconds | INTEGER | NOT NULL | 创建时间 |

## 8) `push_pool`（推题池）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| title | TEXT | NOT NULL | 题目标题 |
| link | TEXT | NOT NULL | 题目链接 |
| description | TEXT | NULL | 题目描述 |
| submitter_id | INTEGER | NOT NULL | 提交者用户 ID |
| status | TEXT | NOT NULL DEFAULT 'PENDING' | 状态：PENDING/APPROVED/PUBLISHED/REJECTED |
| sort_order | INTEGER | NULL | 排序序号（越小越靠前） |
| created_at | DATETIME | NULL | 创建时间 |
| approved_by | INTEGER | NULL | 审批者用户 ID |
| approved_at | DATETIME | NULL | 审批时间 |

索引：`idx_push_pool_status` on `(status)`

## 9) `push_submissions`（推题解答提交）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| push_id | INTEGER | NOT NULL | 推题 ID |
| user_id | INTEGER | NOT NULL | 提交用户 ID |
| submission_link | TEXT | NOT NULL | 解答链接 |
| result_description | TEXT | NULL | 解答描述 |
| created_at | DATETIME | NULL | 提交时间 |

## 10) `daily_push`（每日推送记录）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| date | TEXT | NOT NULL, UNIQUE | 日期 |
| push_id | INTEGER | NOT NULL | 推题 ID |

## 11) `role_change_log`（角色变更审计日志）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| target_user_id | INTEGER | NOT NULL | 被修改用户 ID |
| changed_by | INTEGER | NOT NULL | 操作者用户 ID |
| from_role | TEXT | NULL | 原角色 |
| to_role | TEXT | NOT NULL | 新角色 |
| changed_at | DATETIME | NULL | 变更时间 |

## 12) `cf_rating_change`（Codeforces Rating 比赛历史）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 站内用户 ID |
| contest_id | BIGINT | NOT NULL | Codeforces 比赛 ID |
| contest_name | VARCHAR(255) | NULL | 比赛名称 |
| contest_rank | INTEGER | NULL | 比赛排名 |
| old_rating | INTEGER | NULL | 赛前 Rating |
| new_rating | INTEGER | NULL | 赛后 Rating |
| rating_change | INTEGER | NULL | Rating 变化 |
| rating_update_time_seconds | BIGINT | NULL | Rating 更新时间（Unix 秒） |
| contest_url | VARCHAR(500) | NULL | 比赛链接 |

唯一约束：`(user_id, contest_id)`；成员刷新 Codeforces 资料时按用户替换同步完整历史。

## 13) `problem_list`（个人与共享题单）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 题单 ID |
| owner_user_id | BIGINT | NOT NULL | 创建者用户 ID |
| name | VARCHAR(80) | NOT NULL | 一级题单名称 |
| description | VARCHAR(500) | NULL | 题单简介 |
| is_shared | TINYINT(1) | NOT NULL DEFAULT 0 | 是否全站共享，仅管理员可设为 1 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 最近更新时间 |

## 14) `problem_list_item`（题单题目快照）
| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK AUTO_INCREMENT | 条目 ID |
| list_id | BIGINT | NOT NULL | 所属题单 ID |
| title | VARCHAR(255) | NOT NULL | 保存时的题目标题 |
| link | VARCHAR(1000) | NOT NULL | 题目链接 |
| note | VARCHAR(1000) | NULL | 创建者备注 |
| problem_key | VARCHAR(64) | NULL | Codeforces 题目键 |
| rating | INTEGER | NULL | 保存时的 Rating |
| tags | TEXT | NULL | 保存时的标签快照 |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 题单内顺序 |
| created_at | TIMESTAMP | NOT NULL | 加入时间 |

## 默认账号

系统启动时仅自动创建 SUPER_ADMIN 账号（账密见 `application.yml` 的 `superAdmin` 配置项）。
普通 ADMIN 账号需用户自行注册后由超管提升权限。
