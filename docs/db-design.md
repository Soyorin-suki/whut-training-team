# Database Design

The project uses SQLite for durable state and Redis only for cache data.

Notes:

- SQLite tables are initialized by `SqliteInitializer`
- Relationships are maintained by application logic rather than explicit foreign keys
- Redis is not part of the relational schema described here

## 1. `users`

User profile and ranking metrics.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| username | TEXT | NOT NULL, UNIQUE | username and Codeforces handle |
| email | TEXT | NULL | email |
| password | TEXT | NOT NULL | password |
| role | TEXT | NULL | `USER` or `ADMIN` |
| uid | INTEGER | NULL | Codeforces uid |
| codeforces_rating | INTEGER | NULL | current rating |
| max_rating | INTEGER | NULL | max historical rating |
| is_online | INTEGER | NULL | online status, `0/1` |
| last_online_time_seconds | INTEGER | NULL | last online time in Unix seconds |
| avatar_url | TEXT | NULL | avatar URL |
| score | INTEGER | NOT NULL DEFAULT 0 | daily total score |
| solved_problem_count | INTEGER | NOT NULL DEFAULT 0 | solved problem count |
| hard_solved_problem_count | INTEGER | NOT NULL DEFAULT 0 | hard solved problem count |
| current_streak_days | INTEGER | NOT NULL DEFAULT 0 | current streak |
| longest_streak_days | INTEGER | NOT NULL DEFAULT 0 | longest streak |

Indexes:

- `idx_users_score`
- `idx_users_solved_problem_count`
- `idx_users_hard_solved_problem_count`
- `idx_users_current_streak_days`
- `idx_users_longest_streak_days`

## 2. `cf_problem`

Cached Codeforces problem metadata.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| problem_key | TEXT | NOT NULL, UNIQUE | typically `contestId-index` |
| contest_id | INTEGER | NOT NULL | contest id |
| problem_index | TEXT | NOT NULL | problem index |
| name | TEXT | NOT NULL | title |
| rating | INTEGER | NULL | difficulty |
| tags | TEXT | NULL | comma-separated tags |
| is_interactive | INTEGER | NOT NULL DEFAULT 0 | interactive flag |
| source_contest_id | INTEGER | NULL | source contest id |
| solved_count | INTEGER | NULL | accepted count |
| source_url | TEXT | NOT NULL | original URL |
| last_synced_at | TEXT | NOT NULL | last sync timestamp |

Indexes:

- `idx_cf_problem_rating`
- `idx_cf_problem_contest_idx`

## 3. `daily_problem`

Daily problem snapshot table. There is at most one daily problem per date.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| date | TEXT | NOT NULL, UNIQUE | `YYYY-MM-DD` |
| problem_key | TEXT | NOT NULL | problem key |
| contest_id | INTEGER | NOT NULL | contest id |
| problem_index | TEXT | NOT NULL | problem index |
| name | TEXT | NOT NULL | title |
| rating | INTEGER | NULL | difficulty |
| tags | TEXT | NULL | tags |
| source_url | TEXT | NOT NULL | original URL |
| generated_at | TEXT | NOT NULL | generation time |
| generated_by | TEXT | NOT NULL | generation source such as `scheduler` or `admin` |

Indexes:

- `idx_daily_problem_date`

## 4. `user_daily_status`

Daily check-in results per user.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| user_id | INTEGER | NOT NULL | user id |
| date | TEXT | NOT NULL | check-in date |
| submission_id | INTEGER | NOT NULL | submission id |
| verdict | TEXT | NOT NULL | judged verdict |
| checked_at | TEXT | NOT NULL | verification time |
| score | INTEGER | NOT NULL DEFAULT 1 | awarded score |

Unique constraint:

- `(user_id, date)`

Indexes:

- `idx_user_daily_status_user`

## 5. `user_practice_draw`

Self-practice draw and verification history.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | draw record id |
| user_id | INTEGER | NOT NULL | user id |
| draw_date | TEXT | NOT NULL | draw date |
| problem_key | TEXT | NOT NULL | problem key |
| contest_id | INTEGER | NOT NULL | contest id |
| problem_index | TEXT | NOT NULL | problem index |
| name | TEXT | NOT NULL | title |
| rating | INTEGER | NULL | difficulty |
| tags | TEXT | NULL | tags |
| source_url | TEXT | NOT NULL | original URL |
| drawn_at | TEXT | NOT NULL | draw time |
| submission_id | INTEGER | NULL | submission id |
| verdict | TEXT | NULL | judged verdict |
| checked_at | TEXT | NULL | verification time |

Indexes:

- `idx_user_practice_draw_user_date`

## 6. `problem_like`

Problem-level likes shared across daily, practice, favorites, and detail views.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| user_id | INTEGER | NOT NULL | liking user id |
| problem_key | TEXT | NOT NULL | problem key |
| created_at | TEXT | NOT NULL | like timestamp |

Unique constraint:

- `(user_id, problem_key)`

Indexes:

- `idx_problem_like_problem_key`
- `idx_problem_like_user_id`

## 7. `problem_favorite`

Problem-level favorites.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| user_id | INTEGER | NOT NULL | favoriting user id |
| problem_key | TEXT | NOT NULL | problem key |
| created_at | TEXT | NOT NULL | favorite timestamp |

Unique constraint:

- `(user_id, problem_key)`

Indexes:

- `idx_problem_favorite_problem_key`
- `idx_problem_favorite_user_id`
- `idx_problem_favorite_created_at`

## 8. `daily_problem_comment`

Legacy daily-instance comment table kept for compatibility and audit.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| daily_problem_date | TEXT | NOT NULL | instance date |
| problem_key | TEXT | NOT NULL | problem key |
| user_id | INTEGER | NOT NULL | author user id |
| reply_comment_id | INTEGER | NULL | reply target comment id |
| content | TEXT | NOT NULL | comment content |
| created_at | TEXT | NOT NULL | create time |

Indexes:

- `idx_daily_problem_comment_daily_problem`
- `idx_daily_problem_comment_reply_comment_id`
- `idx_daily_problem_comment_user_id`
- `idx_daily_problem_comment_created_at`

## 9. `problem_comment`

Current shared problem-level comment table.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| problem_key | TEXT | NOT NULL | problem key |
| user_id | INTEGER | NOT NULL | author user id |
| reply_comment_id | INTEGER | NULL | reply target comment id |
| content | TEXT | NOT NULL | comment content |
| created_at | TEXT | NOT NULL | create time |
| legacy_comment_id | INTEGER | UNIQUE | source `daily_problem_comment.id` for idempotent migration |

Indexes:

- `idx_problem_comment_problem_key`
- `idx_problem_comment_reply_comment_id`
- `idx_problem_comment_user_id`
- `idx_problem_comment_created_at`

Migration notes:

- legacy comments are migrated on startup
- old comments from different daily dates are merged by `problemKey`
- `legacy_comment_id` prevents duplicate imports on repeated migration runs
- the legacy table is intentionally retained for rollback and verification

## 10. `auth_token_session`

Stored login session tokens.

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| id | INTEGER | PK AUTOINCREMENT | primary key |
| user_id | INTEGER | NOT NULL | user id |
| access_token | TEXT | NOT NULL, UNIQUE | access token |
| refresh_token | TEXT | NOT NULL, UNIQUE | refresh token |
| access_expired_at_seconds | INTEGER | NOT NULL | access token expiration |
| refresh_expired_at_seconds | INTEGER | NOT NULL | refresh token expiration |
| created_at_seconds | INTEGER | NOT NULL | creation time |

Indexes:

- `idx_auth_token_session_user`
- `idx_auth_token_session_access_exp`
- `idx_auth_token_session_refresh_exp`

## 11. Redis Cache Notes

Redis stores cache data only, not relational source-of-truth data.

Current cache key in use:

- `daily_problem:<YYYY-MM-DD>`

Notes:

- the cached payload contains the shared daily problem portion
- user-specific `checkedIn` and `score` stay in SQLite-backed logic
- cache is invalidated when today's problem is regenerated
