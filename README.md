# BrickDeck

**BrickDeck** is a LEGO collection intelligence platform. It helps collectors and
builders search LEGO sets, view full parts inventories, catalog owned sets and
loose pieces, and — over time — compare sets, find missing pieces, and track deals.

> Personal/portfolio project built with production conventions, on a path toward a
> possible SaaS product.

## Features

- **Set search & details** — Rebrickable-backed search with local caching.
- **Set parts inventory** — import and browse a set's full parts list (part, color, quantity).
- **Authentication** — email/password with stateless JWT.
- **Collection — owned sets** — track status, purchase price, and date.
- **Collection — loose parts** — track quantity by part + color and storage location.

Planned: missing-pieces engine, set comparison, build recommendations, price
tracking, and AI-assisted piece recognition. See [docs/product/features.md](docs/product/features.md).

## Tech Stack

| Area | Stack |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Spring Security, Bean Validation |
| Database | PostgreSQL 16, Flyway |
| Frontend | Next.js (App Router), React 19, TypeScript, MUI + Emotion, TanStack Query, React Hook Form + Zod |
| External data | Rebrickable API |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers · Vitest + React Testing Library |
| Local infra | Docker Compose |

## Architecture (high level)

Next.js web app → Spring Boot REST API → PostgreSQL, with Rebrickable as the
cache-first external catalog source. Backend follows DDD + hexagonal packaging.

```text
apps/
  api/   # Spring Boot backend (Java 21, Maven)
  web/   # Next.js frontend (React, TypeScript, MUI)
docs/    # Documentation (docs-as-code)
```

Details: [docs/architecture/overview.md](docs/architecture/overview.md).

## Quick Start

```bash
# 1. Environment
cp .env.example .env                 # set REBRICKABLE_API_KEY and a strong JWT_SECRET
cp apps/web/.env.example apps/web/.env

# 2. Database (PostgreSQL 16 on localhost:5433)
docker compose up -d

# 3. Backend  -> http://localhost:8080
cd apps/api && ./mvnw spring-boot:run

# 4. Frontend -> http://localhost:3000
cd apps/web && npm install && npm run dev
```

Full guide: [docs/development/setup.md](docs/development/setup.md).

## Main Commands

```bash
# Backend (from apps/api)
./mvnw spring-boot:run                # run API
./mvnw clean verify                   # build + all tests (needs PostgreSQL on 5433)

# Frontend (from apps/web)
npm run dev                           # dev server
npm run test                          # Vitest
npm run typecheck && npm run lint     # quality
```

- API health: <http://localhost:8080/api/v1/health>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>

## Documentation

Everything lives under [`docs/`](docs/README.md):

- [Product](docs/product/vision.md) — vision, roadmap, features, FDDs
- [Architecture](docs/architecture/overview.md) — overview, technical/data/API design, diagrams
- [Decisions](docs/decisions/) — ADRs
- [API](docs/api/README.md) — OpenAPI, conventions, errors, examples
- [Testing](docs/testing/testing-strategy.md) — strategy and plan
- [Development](docs/development/setup.md) — setup, env vars, standards, contribution

## Project Status

**Phase 2 — User Collection (backend complete).** Catalog foundation (Phase 1)
shipped; collection backend (auth, owned sets, loose parts) done. In progress:
frontend auth wiring, then collection UI.

Roadmap: [docs/product/roadmap.md](docs/product/roadmap.md).

## Contributing

Git-flow (`develop` is the default branch), Conventional Commits, tests with every
change. See [docs/development/contribution-guide.md](docs/development/contribution-guide.md).

## License

TODO — not yet defined.
