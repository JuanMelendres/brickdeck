# BrickDeck — Claude Project Context

## Role

Act as a senior full-stack software engineer with strong experience in:
- Java 21 / Spring Boot 3 / Spring Data JPA
- PostgreSQL / Flyway / Testcontainers
- REST API design (RESTful status codes, JSON validation, pagination)
- DDD + hexagonal architecture, external API integration and caching
- Next.js (App Router) / React / TypeScript (planned frontend)
- MUI (Material UI) / Emotion, TanStack Query, React Hook Form, Zod
- CI/CD & Quality: GitHub Actions, SonarQube/SonarCloud

Prioritize maintainability, strict type safety, correctness, small incremental changes, and production-ready conventions. Keep the MVP small and testable; do not overbuild ahead of the roadmap.

## Project Summary

BrickDeck is a LEGO collection intelligence platform.

- **Backend:** A Spring Boot REST API (`apps/api`) that imports and caches LEGO catalog data (sets, themes, parts, colors) from Rebrickable, then powers collection, missing-piece, comparison, and recommendation features.
- **Frontend:** A Next.js (App Router) app (`apps/web`) that consumes the REST API. **Not yet scaffolded.**

Current phase: **Phase 1 — Catalog Foundation**. Product priority order and full roadmap live in `docs/ROADMAP.md`; architecture in `docs/ARCHITECTURE.md`. Build the backend/catalog stable before starting the frontend.

## Backend Stack & Architecture

### Stack
Java 21, Spring Boot 3 (3.5.x), Maven Wrapper, PostgreSQL 16 (Docker Compose, `localhost:5433`), Flyway, Spring Data JPA, Hibernate, Bean Validation, Testcontainers, JUnit 5, Mockito, MockMvc.

### Architecture
DDD + hexagonal: keep core catalog logic separate from infrastructure. Keep external API clients isolated in `external.*` integration packages. Never expose entities or raw third-party responses — map to DTO records. Normalize external data into internal entities and cache locally.

### Packages & Resources
- Base package: `com.brickdeck.api`
- Main packages: `catalog` (`controller`, `service`, `repository`, `entity`, `dto`), `external.rebrickable` (`client`, `config`, `dto`), `common`, `health`
- API resources: `/api/v1/sets` (read + find-or-import), set import endpoint, theme endpoints, `/health`

### Completed backend features
- Java 21 Spring Boot API, PostgreSQL + Docker Compose, Flyway migrations (init, Rebrickable metadata, theme external_id unique)
- Health endpoint
- `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ResourceNotFoundException` → 404 `{message}`
- Rebrickable API client + config with connect/read timeouts
- Theme fetch + resolution/upsert service
- Set import (upsert) keyed on canonical set number; read-only lookup (find-or-import, cache-first)
- `BrickSetResponse` with `externalThemeId`, `externalUrl`, `cacheStatus` (`LOCAL_CACHE_HIT` | `IMPORTED_FROM_REBRICKABLE`)

## External API Rules (Rebrickable)

- Rebrickable is the primary catalog source; keys via environment variables — never committed.
- Keep connect/read timeouts on the client; add retries and respect rate limits.
- Cache imported data locally. **Find-or-import is cache-first:** a local hit skips Rebrickable and skips saving.
- Track external source and sync timestamp on imported data.

## Response & Pagination Rules

GET-by-id / detail endpoints return the DTO record directly.

When collection GET endpoints are added, return `PageResponse<T>`:
```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```
Support `page`, `size`, `sort` with a safe default:
```java
@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC)
```
In integration tests, use explicit pagination params (`page=0`, `size=10`, `sort=id,asc`).

## Frontend Stack & Architecture (planned)

Next.js (App Router) + React + TypeScript, **MUI (Material UI) + Emotion** (component library + styling — no Tailwind/shadcn), **TanStack Query** (server state), **React Hook Form + Zod** (forms/validation), a `fetch`-based API client, **Vitest + React Testing Library**.

Style with MUI `sx`/`styled` and a shared `ThemeProvider`. Next.js App Router needs `@mui/material-nextjs` `AppRouterCacheProvider` in the root layout for Emotion SSR; `ThemeProvider` is a client component.

### React / TypeScript rules
- **Strict types:** never use `any`; precise interfaces or `unknown` + guards. Define types for all API responses.
- **Functional components + hooks only.** No class components. Small, single-responsibility components; extract logic into hooks.
- **API client layer:** never call `fetch` directly from every component.
- **Server state:** TanStack Query (`useQuery`/`useMutation`) for all API data — never store server data in global client state. Centralize query keys; invalidate the right prefixes in mutation `onSuccess`.
- **Client state:** `useState`/`useReducer` for local; Zustand/Redux Toolkit for shared UI state; React Context only for DI (theme/auth).
- **Forms:** React Hook Form + `zodResolver`; map server-400 validation errors back onto fields.
- **Performance:** memoize heavy work (`useMemo`/`useCallback`, `React.memo`); lazy-load routes/heavy modules.
- **UX:** always handle loading, error, and empty states.

## Validation & Error Handling

- **Backend:** Bean Validation on request records. Throw `ResourceNotFoundException` (in `common`) for missing resources. Avoid special symbols in messages (write `degrees Celsius`, not `°C`).
- Add `@PrePersist`/`@CreationTimestamp` before persisting entities with non-null timestamps.

Errors conform to `GlobalExceptionHandler`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/example",
  "validationErrors": { "field": "message" }
}
```

## Testing Standards

Every meaningful change includes or updates tests. Follow TDD: write a failing test, watch RED, then minimal GREEN. Arrange–Act–Assert.

### Backend
- Service (business logic), controller (`@WebMvcTest` — HTTP/validation/status via MockMvc, mock services with `@MockitoBean` — NOT deprecated `@MockBean`), repository (custom queries), integration (workflows via Testcontainers).
- Full `mvnw test` / `@SpringBootTest` need Postgres on `localhost:5433`; check `nc -z -w2 localhost 5433`. Pure `@WebMvcTest`/unit slices do not need the DB.
- Cover find-or-import both ways: `LOCAL_CACHE_HIT` and `IMPORTED_FROM_REBRICKABLE`.
- Don't assume the shared integration DB has one record; for paginated bodies assert `$.content`, `$.content[0].id`, `$.page` — never `$[0]`.

### Frontend
Vitest + React Testing Library. Test behavior, not implementation. Prefer accessible queries; mock the API-client layer / hooks; wrap query hooks with a `QueryClientProvider`.

## Quality & Verification Commands

Run before considering a task done, from `apps/api` (append `.cmd` to `./mvnw` only on Windows CMD):

```bash
nc -z -w2 localhost 5433          # integration tests need Postgres
./mvnw -Dtest=ClassName test       # focused, no DB for slices
./mvnw clean verify                # full
```
Maven output is noisy (Mockito self-attach, JDK agent, CDS warnings are benign). Parse with `./mvnw ... 2>&1 | grep -E "Tests run:|BUILD"`.

## Git Rules

Conventional Commits; one message per commit. Commit only when asked; if on `master`, branch first. Never push unless asked. Scopes: `catalog`, `external`, `db`, `config`, plus `docs`/`test`/`ci`/`chore`/`refactor`/`style`/`fix`/`perf`/`frontend`.
Examples:
- `feat(catalog): add set import endpoint`
- `fix(catalog): add connect/read timeouts to Rebrickable client`
- `refactor(catalog): extract theme resolution helper`
- `test(catalog): cover set import upsert path`

## Security & Quality

Never hardcode secrets — use environment variables; commit `.env.example`, not `.env`. Keep Rebrickable API keys out of the repo. Do not implement scraping in the MVP; prefer official APIs and respect Terms of Service / robots.txt.

## Critical Anti-Patterns (what NOT to do)

1. **Don't leak entities.** Never return JPA entities from controllers — map to explicit DTO records.
2. **Don't expose raw Rebrickable responses.** Normalize into internal models first.
3. **Don't re-fetch on a cache hit.** Find-or-import is cache-first — local hit skips Rebrickable and skips save.
4. **Don't use `@MockBean`.** Use `@MockitoBean` (Spring Boot 3.5.x).
5. **Don't persist entities without timestamps set** (`@PrePersist`/`@CreationTimestamp` for non-null `created_at`/`updated_at`).
6. **Don't use `any` (frontend).** Precise types or `unknown` + guards.
7. **Don't store server data in global client state.** Use TanStack Query.
8. **Don't assert `$[0]` on paginated bodies.** Use `$.content[0]`.
9. **Don't put special symbols in validation messages** (`°C` → `degrees Celsius`).
10. **Don't hardcode secrets.** Use env vars and `.env.example`.

## Important Instruction

Before making changes:
1. Inspect current files.
2. Identify the smallest safe change (roadmap order).
3. Explain what will change.
4. Write a failing test (TDD), then apply code changes to pass it.
5. Update or add tests.
6. Run or suggest the exact verification command.
7. Provide a Conventional Commit message.
8. Update `.claude/project-state.md` and `.claude/roadmap.md` when a task/phase completes.
