# BrickDeck Project State

## Last Updated

2026-07-06

## Current Phase

Phase 1 (Catalog Foundation) in progress. Phase 0 (Foundation) complete. Backend catalog slice is being built out: Rebrickable client, theme + set import (upsert), and read-only lookup endpoints. Frontend (`apps/web`) not yet scaffolded.

## Completed

- Java 21 Spring Boot API (`apps/api`, `com.brickdeck.api`)
- PostgreSQL via Docker Compose (localhost:5433)
- Flyway migrations (V1 init, V2 Rebrickable metadata, V3 theme external_id unique)
- API health endpoint
- `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ResourceNotFoundException` → 404
- Rebrickable API client + config (`external.rebrickable`), connect/read timeouts
- Theme fetch client method + theme resolution/upsert service
- Set import upsert keyed on canonical set number
- Read-only set/theme lookup endpoints (`/api/v1/sets`, find-or-import from Rebrickable)
- `BrickSetResponse` with `externalThemeId`, `externalUrl`, `cacheStatus`
- Catalog controllers: BrickSet, BrickSetLookup, BrickSetImport, Theme
- Set search endpoint (`GET /api/v1/sets/search?q=&page=&size=`) → Rebrickable fuzzy search, `PageResponse<BrickSetResponse>`, 0-indexed (converts to Rebrickable 1-indexed), `cacheStatus=EXTERNAL_SEARCH_RESULT`
- `PageResponse<T>` envelope in `common` (0-indexed, `PageResponse.of(...)` factory)
- Set-number normalization: bare number (`42232`) → default variant (`42232-1`) on exact lookup/import
- Design + implementation plan docs for set import
- Per-repo napkin runbook

## Recently Worked On

- Set search endpoint + `PageResponse<T>` + set-number suffix normalization
- Connect/read timeouts on Rebrickable client (commit 3ded4c0)
- Set import endpoint + read endpoint made read-only (61d3a75)
- Set upsert keyed on canonical set number; deprecated shim marked (71de692)
- Theme resolution + upsert service (aee49a7)

## Known Rules

- Find-or-import is cache-first: local hit skips Rebrickable and skips save.
- Full `mvnw test` needs live Postgres on 5433; run `@WebMvcTest`/unit slices without DB.
- Use `@MockitoBean`, not `@MockBean` (Spring Boot 3.5.x).
- Entities need `@PrePersist`/`@CreationTimestamp` before persisting (non-null timestamps).
- Throw `ResourceNotFoundException` for missing resources.
- TDD mandatory (superpowers): failing test → RED → minimal GREEN.

## Immediate Next Steps

1. Complete Phase 1: set inventory (parts, colors) import + read.
2. Apply `PageResponse<T>` + `@PageableDefault` to `GET /api/v1/sets` (currently a raw `List`).
3. Scaffold `apps/web` (Next.js) once catalog read API is stable.
4. Add Postman collection under `docs/postman` for the catalog API.
