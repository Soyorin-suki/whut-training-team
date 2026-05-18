# Docker

## Topology

- `frontend` is the only host-facing entrypoint and exposes `${FRONTEND_PORT}:80` with Nginx.
- `frontend` proxies `/api/*` to `backend:8080` on the internal `app` bridge network.
- `backend` stores SQLite data in `backend-data:/app/data` and logs in `backend-logs:/app/logs`.
- `redis` is internal-only and backs the shared daily-problem cache with `redis-data:/data`.

## Configuration

1. Copy `.env.docker.example` to `.env` if you need custom ports or runtime variables.
2. The default values are already safe for local startup, so `.env` is optional.

Key variables:

- `FRONTEND_PORT`: host port for the Nginx frontend, default `5173`.
- `SPRING_DATASOURCE_URL`: backend SQLite URL, default `jdbc:sqlite:/app/data/training.db`.
- `SPRING_DATA_REDIS_HOST`: Redis hostname inside compose, default `redis`.
- `SPRING_DATA_REDIS_PORT`: Redis port inside compose, default `6379`.
- `APP_DAILY_PROBLEM_CACHE_TTL`: daily-problem cache TTL, default `1d`.
- `JAVA_OPTS`: optional JVM flags for the backend container.
- `VITE_API_BASE_URL`: optional build-time frontend API base URL. Leave empty for same-origin `/api`.

## Start Or Rebuild

```bash
docker compose up -d --build
```

This is the repo's canonical rebuild command after backend, frontend, or Docker changes.

## Verification

Static validation:

```bash
docker compose config
```

Runtime checks:

```bash
docker compose ps
```

Compose should report all three services as healthy. The backend and Redis healthchecks run inside the internal network, so the host-side verification path is:

- frontend health: `http://localhost:5173/healthz`
- backend health through the frontend proxy: `http://localhost:5173/api/health`

Useful local URLs after startup:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:5173/api/health`

## Operations

View logs:

```bash
docker compose logs -f frontend
docker compose logs -f backend
docker compose logs -f redis
```

Stop without deleting volumes:

```bash
docker compose down
```

Reset containers and persistent data:

```bash
docker compose down -v
```

Use `down -v` carefully. It removes SQLite and Redis volumes, so cached data and persisted users are rebuilt from scratch.

## Notes

- The compose file now centralizes shared restart, init, network, and log-rotation settings through a common service block.
- Frontend and backend images are tagged as local compose images: `whut-training-team/frontend:local` and `whut-training-team/backend:local`.
- The backend image build uses `backend/docker-settings.xml` plus Maven retry flags to make dependency downloads less fragile during container builds.
- `backend/.dockerignore` excludes the workspace-local `.m2` cache so Maven artifacts do not bloat the build context.
- Rebuilding containers does not reset the SQLite database unless you also remove `backend-data`.
