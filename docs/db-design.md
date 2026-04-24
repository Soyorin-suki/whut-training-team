# 库表设计

当前项目运行时仍可使用内存会话；以下是推荐的 SQLite 落库方案（`access token` 校验 + `refresh token` 刷新）。

## 1) `users` 用户表

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 用户主键 |
| username | TEXT | NOT NULL, UNIQUE, length <= 50 | 用户名 |
| email | TEXT | NULL | 邮箱 |
| password | TEXT | NOT NULL | 密码（生产环境应保存哈希） |
| role | TEXT | NULL, CHECK(role in ('USER','ADMIN')) | 角色 |
| created_at | TEXT | NULL | 创建时间 |

## 2) `auth_refresh_tokens` 刷新令牌会话表

建议不存明文 token，保存 `refresh_token_hash`。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 会话主键 |
| user_id | INTEGER | NOT NULL | 关联用户 |
| refresh_token_hash | TEXT | NOT NULL, UNIQUE | Refresh Token 哈希 |
| access_token_id | TEXT | NOT NULL | 当前 access 的标识（如 jti） |
| expires_at | TEXT | NOT NULL | Refresh 过期时间 |
| revoked_at | TEXT | NULL | 撤销时间（登出/风控） |
| replaced_by_token_hash | TEXT | NULL | 轮换后的新 token 哈希 |
| created_at | TEXT | NOT NULL | 创建时间 |
| updated_at | TEXT | NOT NULL | 更新时间 |

## 3) （可选）`auth_access_blacklist`

若 access 使用 JWT 且需要“立即失效”，可加黑名单表。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| jti | TEXT | PRIMARY KEY | access token 唯一标识 |
| user_id | INTEGER | NOT NULL | 关联用户 |
| expires_at | TEXT | NOT NULL | access 过期时间 |
| created_at | TEXT | NOT NULL | 创建时间 |

## SQLite 建表 SQL（建议）

```sql
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE CHECK (length(username) <= 50),
  email TEXT,
  password TEXT NOT NULL,
  role TEXT CHECK (role IN ('USER', 'ADMIN')),
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
