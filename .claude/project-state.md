# BrickDeck Project State

## Last Updated

2026-07-06

## Current Phase

Phase 1 (Catalog Foundation) complete. Phase 0 (Foundation) complete. Backend catalog slice done (Rebrickable client, theme + set import, read-only lookup, search, set inventory, CORS, OpenAPI). Frontend (`apps/web`) scaffolded with MUI + set search page. Next: Phase 2 (User Collection) or frontend set-detail/parts-inventory slice.

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
- `GET /api/v1/sets` paginated: `PageResponse<BrickSetResponse>` via `Pageable` + `@PageableDefault(size=20, sort="externalSetNumber")`
- Set inventory schema foundation (slice 1): `colors`, `parts`, `set_parts` tables (V4) + `Color`/`Part`/`SetPart` entities + repositories; normalized reference model, `set_parts` unique on `(set_id, part_id, color_id, is_spare)`
- Set inventory slice 2: Rebrickable `getSetParts(setNumber, page, pageSize)` client method (`/lego/sets/{num}/parts/`, paginated) + external DTOs `RebrickableSetPartResponse`/`RebrickablePartResponse`/`RebrickableColorResponse`
- Set inventory slice 3: `SetInventoryService.importInventory(setNumber)` — set must exist locally (option A), fetch-all pages, upsert Color/Part reference data (`ColorService`/`PartService`) + `set_parts` lines idempotently (find-by set/part/color/spare before insert); `InventoryImportResult` summary; `SetPartRepository.findByBrickSetAndPartAndColorAndSpare`
- Set inventory slice 4 (complete): `POST /api/v1/catalog/sets/{setNumber}/inventory/import` (200 + `InventoryImportResult`) and `GET /api/v1/sets/{setNumber}/parts` → `PageResponse<SetPartResponse>` (`@PageableDefault size=50 sort=id`); `SetInventoryService.findInventory` + `SetPartResponse` DTO. **Phase 1 "view a set's parts inventory" met.**
- Set-number normalization: bare number (`42232`) → default variant (`42232-1`) on exact lookup/import
- Global CORS config: `CorsConfig` (`WebMvcConfigurer`) + `CorsProperties` (`brickdeck.cors.allowed-origins`, env `CORS_ALLOWED_ORIGINS`, default `http://localhost:3000`) mapping `/api/**`; `allowCredentials=false` (no auth until Phase 2). Pre-frontend prerequisite.
- OpenAPI/Swagger: `springdoc-openapi-starter-webmvc-ui` 2.8.6 + `OpenApiConfig` (`OpenAPI` bean, title "BrickDeck Catalog API", version v1). Spec at `/v3/api-docs`, UI at `/swagger-ui/index.html`. Pre-frontend contract source (TS type generation).
- Frontend scaffold (`apps/web`): Next.js 16 (App Router, src dir) + React 19 + TS strict + **MUI v9/Emotion** (NOT Tailwind/shadcn — see memory) + TanStack Query + RHF+Zod + Vitest/RTL. Typed `apiGet` client (`NEXT_PUBLIC_API_BASE_URL`), `searchSets`, `useSetSearch` hook, `SetSearchBar`/`SetResults`/`SetCard`, `/sets` search page with loading/error/empty/pagination states. MUI SSR via `AppRouterCacheProvider` (`v16-appRouter`) + `ThemeProvider`/`QueryProvider`. TDD throughout.
- Frontend set-detail slice (`/sets/[setNumber]`): `apiPost` + `getSetByNumber`/`getSetParts`/`importSetInventory`; `useSetDetail`/`useSetParts` queries + `useImportInventory` mutation (invalidates `["sets","parts",setNumber]` prefix on success); `SetDetail` (set info) + `PartsInventory` (MUI table, loading/error/empty states, prev/next pagination, "Import inventory" button when empty). `SetCard` name links to detail page. 32 tests total (18 new).
- Design + implementation plan docs for set import
- Per-repo napkin runbook

## Recently Worked On

- OpenAPI-generated frontend types: `openapi-typescript` → `src/lib/types/schema.d.ts` (from `/v3/api-docs`); `lib/types/api.ts` now derives `BrickSetResponse`/`SetPartResponse`/`InventoryImportResult` from the schema (via `Nullable<T>` — DTOs have no OpenAPI nullability metadata but Jackson emits nulls). `PageResponse<T>` stays hand-written (OpenAPI can't express the generic). `npm run gen:api` refreshes against the running API.
- Frontend set-detail + parts-inventory slice (`/sets/[setNumber]`, import button) — TDD (32 tests), typecheck/lint/build green
- Frontend scaffold + set search page (`apps/web`, MUI/Next 16) — completes Phase 1; TDD, typecheck/lint/build green
- OpenAPI/Swagger (`springdoc` 2.8.6 + `OpenApiConfig`) — pre-frontend API contract; TDD via `OpenApiDocsTest`
- Global CORS config (`CorsConfig`/`CorsProperties`) — pre-frontend prerequisite; TDD via `CorsConfigTest` preflight slice
- Set inventory slice 4: import + read endpoints (completes set inventory feature + Phase 1 core criterion)
- Set inventory slice 3: idempotent import service (ColorService/PartService/SetInventoryService)
- Set inventory slice 2: Rebrickable set-parts fetch client + external DTOs
- Set inventory schema foundation: colors/parts/set_parts tables + entities + repos (slice 1 of set inventory)
- Paginated `GET /api/v1/sets` with `PageResponse` + `@PageableDefault`
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

1. Add Postman collection under `docs/postman` for the catalog API (can derive from OpenAPI spec).
2. Consider marking backend DTO fields required/nullable so generated types drop the `Nullable<T>` workaround.
3. Begin Phase 2 (User Collection): auth + add-set-to-collection.
