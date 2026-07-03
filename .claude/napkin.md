# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-07-03] Full `mvnw test` needs live Postgres**
   `ApiApplicationTests` is `@SpringBootTest` (ddl-auto=validate + Flyway) → needs Postgres on `localhost:5433`.
   Do instead: `nc -z -w2 localhost 5433` first. For pure unit/`@WebMvcTest` slices run `./mvnw -Dtest=ClassA,ClassB test` (no DB).
2. **[2026-07-03] TDD is mandatory here (superpowers)**
   Do instead: write failing test → watch RED (`mvnw -Dtest=X test`) → minimal GREEN. Never write prod code first.
3. **[2026-07-03] Maven output is noisy**
   Mockito self-attach + JDK agent + CDS warnings are benign.
   Do instead: parse results with `./mvnw ... 2>&1 | grep -E "Tests run:|BUILD"`.

## Shell & Command Reliability
1. **[2026-07-03] Always use `./mvnw` wrapper from `apps/api/`**
   Do instead: `cd apps/api` then `./mvnw`, not system `mvn`.

## Domain Behavior Guardrails
1. **[2026-07-03] Spring Boot 3.5.x → `@MockitoBean`**
   `@MockBean` deprecated.
   Do instead: use `org.springframework.test.context.bean.override.mockito.MockitoBean` in `@WebMvcTest`.
2. **[2026-07-03] Entity timestamps lack lifecycle hooks**
   `BrickSet`/`Theme` mark `created_at`/`updated_at` `nullable=false` with no `@PrePersist`/`@CreationTimestamp` → insert NPE risk. `source` set in service.
   Do instead: add `@PrePersist`/`@CreationTimestamp` before any code path that persists.
3. **[2026-07-03] Catalog API surface (evolved by user)**
   Endpoints under `/api/v1/sets` (`GET` list, `GET /by-number/{setNumber}` = find-or-import from Rebrickable). `BrickSetResponse` carries `externalThemeId`, `externalUrl`, `cacheStatus` (`LOCAL_CACHE_HIT` | `IMPORTED_FROM_REBRICKABLE`).
   Do instead: reuse this shape; keep `find-or-import` cache-first (local hit skips Rebrickable + skips save).
4. **[2026-07-03] 404 mapping**
   `ResourceNotFoundException` (`common`) → `GlobalExceptionHandler` `@RestControllerAdvice` returns 404 `{message}`.
   Do instead: throw `ResourceNotFoundException` from services for missing resources.

## User Directives
1. **[2026-07-03] Conventional Commits (global CLAUDE.md)**
   Do instead: `feat(catalog): ...`, `refactor(...)`. Commit only when asked; if on `master`, branch first.
2. **[2026-07-03] Caveman prose mode active**
   Do instead: terse prose; write code/commits/PRs/security normally.
3. **[2026-07-03] Architecture rules**
   DDD + hexagonal; DTOs are Java records; never expose entities or raw 3rd-party responses; Rebrickable is primary source, keys via env.