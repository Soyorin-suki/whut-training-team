# WHUT Training Team

竞赛编程训练平台 —— 基于 Codeforces 的每日打卡、自主练习与推题系统。

- 后端：Spring Boot 3.3 + Maven + SQLite（JdbcTemplate，无 JPA）
- 前端：React 18 + Vite + Tailwind CSS + Radix UI + react-router-dom v6
- 认证：Access Token + Refresh Token 双重令牌
- 用户分级：`USER` / `ADMIN` / `SUPER_ADMIN` 三级权限

## 项目结构

```text
whut-training-team
├── backend
│   ├── src/main/java/com/whut/training
│   │   ├── aspect/annotation        # @ServiceLog 注解
│   │   ├── aspect/logging           # ServiceLogAspect AOP 日志切面
│   │   ├── common                   # ApiResponse、Codeforces 工具
│   │   ├── config                   # WebConfig、DataInitializer、SqliteInitializer
│   │   ├── controller               # 控制层（11 个 Controller）
│   │   ├── context                  # UserContext（ThreadLocal）
│   │   ├── domain/dto               # 请求/响应 DTO
│   │   ├── domain/entity            # 实体（record，User 除外）
│   │   ├── domain/enums             # UserRole 枚举
│   │   ├── exception                # BusinessException + GlobalExceptionHandler
│   │   ├── interceptor              # AccessTokenInterceptor + RequestLoggingInterceptor
│   │   ├── repository               # 数据访问层（JdbcTemplate）
│   │   ├── service                  # 业务接口
│   │   │   └── impl                 # 业务实现
│   │   └── utils                    # TokenUtils
│   └── src/main/resources/application.yml
├── frontend
│   └── src
│       ├── api                      # 接口请求封装（auth/user/dailyProblem/home/leaderboard/push/admin）
│       ├── components/layout        # AppLayout（Sidebar + TopBar + Outlet）
│       ├── components/ui            # 通用 UI 组件（ProblemCard/Heatmap/Pagination/Skeleton 等）
│       ├── context                  # AuthContext（useReducer + localStorage）
│       ├── pages                    # 页面组件（Home/Login/Register/DailyProblem/Practice/Profile/Leaderboard/Push）
│       │   └── admin                # 管理页面（AdminDaily/AdminUsers/AdminPush）
│       └── utils                    # Codeforces 工具函数
└── docs
    ├── api-design.md                # 接口设计文档
    ├── db-design.md                 # 库表设计（11 张表）
    └── todolist.md                  # 历史待办记录
```

## 文档索引

| 文档 | 内容 |
|------|------|
| [docs/api-design.md](./docs/api-design.md) | 前后端接口文档（鉴权分级、所有端点） |
| [docs/db-design.md](./docs/db-design.md) | 库表设计（11 张表、字段说明、索引） |
| [require.md](./require.md) | 前端设计需求与风格约束 |
| [docs/todolist.md](./docs/todolist.md) | 历史开发待办记录 |

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 用户系统 | 注册（CF handle 校验）、登录、token 刷新/注销、资料修改（昵称/头像/简介）、三级权限 |
| 每日一题 | 每日两题（easy/hard），rating 阈值分割，打卡校验（Codeforces 提交），积分 = 题目 rating，支持管理员重生成/重抽 |
| 排行榜 | 按总积分排序，支持分页，平局按最后打卡时间 |
| 自主练习 | 按 rating 范围随机抽题，提交校验（不计分），支持历史记录查看与删除 |
| 首页聚合 | 活跃用户数、排行榜 Top N、今日题目（easy+hard）、今日推题、打卡统计、热力图（365 天） |
| 推题系统 | 用户提交 → 管理员审核 → 推题池管理 → 每日定时推送 → 用户提交解答，支持推题历史查看 |
| 热力图 | GitHub 风格贡献热力图（365 天），colorLevel 0-4，首页+个人页统一 |
| 管理员 | 创建用户、推题审核、重生成/重抽每日题、推题池管理（删除/提升） |
| 超管理员 | 修改任意用户角色（USER/ADMIN/SUPER_ADMIN），含审计日志 |

## 启动方式

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 注意事项

- **首次使用请先注册账号**（注册即获得 `USER` 角色）
- **提升权限**：使用 SUPER_ADMIN 账号登录，在管理面板中修改目标用户的角色
- **SUPER_ADMIN 账密**：在 `backend/src/main/resources/application.yml` 的 `superAdmin` 配置项中
- 系统首次启动时会自动创建 SUPER_ADMIN 账号（若不存在），不创建普通 ADMIN
