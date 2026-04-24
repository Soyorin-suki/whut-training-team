# 库表设计

当前后端使用 SQLite。下面是当前版本的核心表结构说明。

## 1) `users` 用户表

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 用户主键 |
| username | TEXT | NOT NULL, UNIQUE, length <= 50 | 用户名 |
| email | TEXT | NULL | 邮箱 |
| password | TEXT | NOT NULL | 密码（生产环境应存哈希） |
| role | TEXT | NULL, CHECK(role in ('USER','ADMIN')) | 角色 |
| uid | INTEGER | NULL | 从 Codeforces 头像链接解析出的 uid |
| codeforces_rating | INTEGER | NULL | Codeforces 当前 rating |
| max_rating | INTEGER | NULL | Codeforces 历史最高 rating |
| is_online | INTEGER | NULL | 是否在线（0/1） |
| last_online_time_seconds | INTEGER | NULL | 最近在线时间（Unix 秒） |
| avatar_url | TEXT | NULL | 头像 URL |
| created_at | TEXT | NULL | 创建时间（示例 SQL 中有默认值） |

## 2) `auth_refresh_tokens`（预留方案）

当前项目主要使用内存会话。若后续落库 refresh token，可采用以下结构：

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 会话主键 |
| user_id | INTEGER | NOT NULL | 关联用户 |
| refresh_token_hash | TEXT | NOT NULL, UNIQUE | Refresh Token 哈希 |
| access_token_id | TEXT | NOT NULL | Access 标识（如 jti） |
| expires_at | TEXT | NOT NULL | 过期时间 |
| revoked_at | TEXT | NULL | 撤销时间 |
| replaced_by_token_hash | TEXT | NULL | 轮换后的新 token 哈希 |
| created_at | TEXT | NOT NULL | 创建时间 |
| updated_at | TEXT | NOT NULL | 更新时间 |

## 3) `auth_access_blacklist`（可选）

如果 access token 使用 JWT 且需要立即失效能力，可以加入黑名单表：

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| jti | TEXT | PRIMARY KEY | Access token 唯一标识 |
| user_id | INTEGER | NOT NULL | 关联用户 |
| expires_at | TEXT | NOT NULL | Access 过期时间 |
| created_at | TEXT | NOT NULL | 创建时间 |

## SQLite 建表示例 SQL

```sql
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE CHECK (length(username) <= 50),
  email TEXT,
  password TEXT NOT NULL,
  role TEXT CHECK (role IN ('USER', 'ADMIN')),
  uid INTEGER,
  codeforces_rating INTEGER,
  max_rating INTEGER,
  is_online INTEGER,
  last_online_time_seconds INTEGER,
  avatar_url TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  refresh_token_hash TEXT NOT NULL UNIQUE,
  access_token_id TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  revoked_at TEXT,
  replaced_by_token_hash TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
ON auth_refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
ON auth_refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS auth_access_blacklist (
  jti TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL,
  expires_at TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_access_blacklist_expires_at
ON auth_access_blacklist(expires_at);
```

