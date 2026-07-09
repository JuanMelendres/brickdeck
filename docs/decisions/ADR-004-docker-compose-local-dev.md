# ADR-004: Docker Compose for Local Development

## Status
Accepted

## Date
2026-07-02

## Context
Local development needs a reproducible PostgreSQL instance without requiring each
developer to install and configure a database manually.

## Decision
Provide a root `docker-compose.yml` that runs `postgres:16-alpine` with a named
volume for persistence, mapping container `5432` to host `5433`. Credentials come
from the compose file (dev defaults) and are overridable via environment variables.

## Decision Scope
Only PostgreSQL is containerized today. The API and web app run on the host
(`./mvnw spring-boot:run`, `npm run dev`). Containerizing the apps is future scope.

## Consequences
- Positive: `docker compose up -d` gives a ready database in seconds.
- Positive: data persists across restarts via the named volume.
- Negative: apps are not yet containerized, so "full stack in Docker" is not available.

## Alternatives Considered
- Local native PostgreSQL install: rejected — inconsistent across machines.
- Full compose stack (api + web + db): deferred until deployment work.

## Notes
See [development/setup.md](../development/setup.md).
