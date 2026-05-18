# WHUT Training Team

WHUT Training Team is a full-stack training portal built with:

- Backend: Spring Boot + Maven
- Frontend: React + Vite
- Persistence: SQLite
- Cache: Redis
- Auth: Access Token + Refresh Token

Current feature set includes:

- Daily problem and self-practice flows
- Shared problem likes and favorites
- Dedicated problem detail page
- Shared problem-level comments keyed by `problemKey`
- Leaderboards and streak metrics
- Admin training dashboard
- Admin AI original problem workbench
- Docker Compose based local deployment

## Repository Layout

```text
whut-training-team
|-- backend
|   |-- src/main/java/com/whut/training
|   |   |-- common
|   |   |-- config
|   |   |-- controller
|   |   |-- domain
|   |   |   |-- dto
|   |   |   |-- entity
|   |   |   `-- enums
|   |   |-- exception
|   |   |-- interceptor
|   |   |-- repository
|   |   |-- service
|   |   `-- utils
|   |-- Dockerfile
|   `-- src/main/resources/application.yml
|-- frontend
|   |-- src
|   |   |-- api
|   |   `-- views
|   |-- Dockerfile
|   `-- nginx.conf
|-- docs
|   |-- api-design.md
|   |-- db-design.md
|   `-- docker.md
|-- docker-compose.yml
`-- .env.docker.example
```

## Local Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

- Default URL: `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

- Default URL: `http://localhost:5173`

## Docker

```bash
docker compose up -d --build
```

Recommended browser entrypoints:

- Frontend: `http://localhost:5173`
- Frontend health: `http://localhost:5173/healthz`
- Backend health through frontend proxy: `http://localhost:5173/api/health`

See [docs/docker.md](docs/docker.md) for the full Docker runbook.

## Default Admin Account

- `username`: `admin`
- `password`: `admin123`

The application auto-creates this account on startup when it does not exist yet.

## AI Workbench Config

To enable admin AI problem generation, set:

- `APP_LLM_DEFAULT_PROVIDER`
- `APP_LLM_BASE_URL`
- `APP_LLM_API_KEY`
- `APP_LLM_MODEL`
- `APP_AI_PROBLEM_STORAGE_ROOT` (optional)

Without these values, normal portal features still work and the AI endpoints return a clear config error.

## Problem Detail And Shared Comments

- The unified frontend detail route is `#/problems/:problemKey`
- Daily, history, practice, and favorites all open the same detail page
- Comments are now owned by `problemKey`, not by daily-instance date
- Legacy `daily_problem_comment` rows are migrated into `problem_comment`

## Documentation

- [API Design](docs/api-design.md)
- [Database Design](docs/db-design.md)
- [Docker Runbook](docs/docker.md)
