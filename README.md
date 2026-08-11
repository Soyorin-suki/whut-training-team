# WHUT Training Team

竞赛编程训练平台 —— 基于 Codeforces 的每日打卡、自主练习与推题系统。

- 后端：Spring Boot 3.3 + Maven + MySQL 8（JdbcTemplate，无 JPA）
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
│   │   ├── config                   # WebConfig、DataInitializer、MySqlInitializer
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
| [docs/deployment.md](./docs/deployment.md) | 生产环境部署、安全配置、Nginx 与 systemd |
| [require.md](./require.md) | 前端设计需求与风格约束 |
| [docs/todolist.md](./docs/todolist.md) | 历史开发待办记录 |

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 用户系统 | 登录账号与展示用户名分离、Codeforces/AtCoder 所有权验证绑定、token 刷新/注销、资料修改、三级权限 |
| 每日一题 | 每日两题（easy/hard），rating 阈值分割，打卡校验（Codeforces 提交），积分 = 题目 rating，支持管理员重生成/重抽 |
| 排行榜 | 按总积分排序，支持分页，平局按最后打卡时间 |
| 自主练习 | 按 rating 与多个 Codeforces 标签组合抽题（标签为 AND 匹配），仅记录抽题历史，不进行提交校验 |
| 个人题单 | 用户创建一级专题题单并保存题目；每日题和自主练习可一键加入；管理员可发布全站只读共享题单 |
| AtCoder 周赛 | 自动发现每周 ABC，赛前更新并在开赛时冻结现役名单，按官方参赛历史与赛时 AC 检查完成情况，支持豁免、重试与 Excel 导出 |
| 首页聚合 | 活跃用户数、排行榜 Top N、今日题目（easy+hard）、今日推题、打卡统计、热力图（365 天） |
| 推题系统 | 用户提交 → 管理员审核 → 推题池管理 → 每日定时推送 → 用户提交解答，支持推题历史查看 |
| 热力图 | GitHub 风格贡献热力图（365 天），colorLevel 0-4，首页+个人页统一 |
| 管理员 | 创建用户、推题审核、重生成/重抽每日题、推题池管理（删除/提升） |
| 超管理员 | 修改任意用户角色（USER/ADMIN/SUPER_ADMIN），含审计日志 |

## 启动方式

### 1. 启动 MySQL

已安装 Docker 时，推荐直接在项目根目录运行：

```bash
docker compose up -d mysql
```

默认会创建 `whut_training` 数据库，用户名为 `root`，开发密码为
`whut_dev_password`。数据保存在 Docker volume 中。

如果使用本机已有 MySQL，请设置以下环境变量：

```text
MYSQL_URL=jdbc:mysql://localhost:3306/whut_training?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的密码
```

MySQL 账号需要拥有创建 `whut_training` 数据库和表的权限。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

后端连接成功后会自动创建空表和 SUPER_ADMIN，不会读取或迁移旧 SQLite 数据。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 注意事项

- **首次使用请先注册账号**（注册即获得 `USER` 角色）
- **绑定 Codeforces**：登录后进入个人中心，输入 Handle，并在 2 分钟内向 Codeforces 1A 提交一次编译错误以完成所有权验证
- **绑定 AtCoder**：个人中心生成验证码后，将验证码临时填写到 AtCoder Settings 的 `Affiliation`，再回到平台完成验证
- **ABC 判定**：默认要求正式比赛时段内至少 AC 1 题；管理员可在训练看板调整最低 AC 数和最终判定等待时间
- **数据来源**：参赛信息来自 AtCoder 官方公开 History；赛时 AC 来自 AtCoder Problems 公共接口。外部接口异常会标记“数据异常”并自动重试，不会误判为缺席
- **并发与外部限流**：日题校验采用异步任务，Codeforces 请求统一限速并让打卡优先；资料刷新、比赛聚合和打卡分别使用有界线程池。当前配置不依赖 Redis，适合单实例约 20 人并发使用
- **提升权限**：使用 SUPER_ADMIN 账号登录，在管理面板中修改目标用户的角色
- **生产 SUPER_ADMIN 账密**：通过 `SUPERADMIN_USERNAME` 和 `SUPERADMIN_PASSWORD` 环境变量提供，禁止沿用开发默认值
- 系统首次启动时会自动创建 SUPER_ADMIN 账号（若不存在），不创建普通 ADMIN

## 生产发布

生产环境必须启用 `prod` profile，并通过环境变量提供数据库、超级管理员和 CORS 配置。完整操作见 [生产部署说明](./docs/deployment.md)。仓库中的 `deploy/` 提供了 Nginx、systemd 和环境变量模板；`.github/workflows/ci.yml` 会在推送和 Pull Request 时运行后端测试、前端依赖审计与生产构建。
