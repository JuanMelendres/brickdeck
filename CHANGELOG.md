# Changelog

All notable changes to BrickDeck are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project is
pre-release and not yet versioned (see the roadmap for phases).

## [Unreleased]

### Added
- Missing-pieces engine (Phase 3a, backend): authenticated
  `GET /api/v1/sets/{setNumber}/missing-parts` returning a `MissingPartsReport`
  (required vs owned per part+color and completion percentage). Owned combines
  loose parts and the parts of owned/built/in-progress sets; spares count toward
  owned, not required.
- Full Phase 2 frontend: auth wiring (token store, Bearer client, login/register,
  AuthProvider, route guard, nav/logout) and collection UI (owned sets + loose
  parts) in `apps/web`.
- CI pipeline (GitHub Actions): backend `mvnw verify` against a PostgreSQL
  service, and frontend lint/typecheck/test/build.
- Documentation reorganized into a docs-as-code structure under `docs/`
  (product, architecture, decisions, api, testing, development) with ADRs,
  a static OpenAPI spec, and lightweight FDDs.

## Phase 2 — User Collection (backend complete)

### Added
- Loose parts inventory: `user_parts` (V7) and `POST/GET/PATCH/DELETE /api/v1/collection/parts`.
- Owned sets: `user_sets` (V6) and `POST/GET/PATCH/DELETE /api/v1/collection/sets`,
  with status, purchase price/date, duplicate `409`, owner-scoped `404`.
- Authentication: stateless JWT, `users` (V5), `POST /api/v1/auth/register` + `/login`, `GET /me`.
- Frontend: API types derived from the backend OpenAPI spec.

## Phase 1 — Catalog Foundation

### Added
- Frontend scaffold (`apps/web`, Next.js + MUI) with set search and set-detail/parts pages.
- OpenAPI spec + Swagger UI (springdoc).
- Global CORS configuration for `/api/**`.
- Set inventory: `colors`/`parts`/`set_parts` schema (V4), Rebrickable set-parts
  client, idempotent import service, and import/read endpoints.
- Set search endpoint + `PageResponse<T>` envelope; paginated `GET /api/v1/sets`.
- Set import (upsert) + read-only find-or-import lookup; theme resolution/upsert.
- Rebrickable API client with connect/read timeouts.

## Phase 0 — Foundation

### Added
- Spring Boot API (`apps/api`, Java 21) with health endpoint and `GlobalExceptionHandler`.
- PostgreSQL via Docker Compose; Flyway migrations (V1–V3).
- Repository structure, documentation, and Claude configuration.
