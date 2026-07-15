# BrickDeck Roadmap (working summary)

Full roadmap: `docs/ROADMAP.md`. This is the condensed working view.

## Phase 0 — Foundation

Status: Completed

- Repository, README, docs (architecture, API, AI, scraping strategy)
- Project structure (`apps/api`, `apps/web`, `docs`, `infra`)
- Claude configuration
- Docker Compose + environment strategy

## Phase 1 — Catalog Foundation

Status: Completed

- Spring Boot API + PostgreSQL + Flyway — Done
- Rebrickable API client — Done
- Theme upsert + resolution — Done
- Set import (upsert) + read-only lookup — Done
- Catalog search endpoint (Rebrickable-backed, `PageResponse`) — Done
- Set inventory (parts, colors) import + read — Done (schema, client, import service, endpoints)
- Global CORS config for `/api/**` (pre-frontend) — Done
- OpenAPI spec + Swagger UI (springdoc, pre-frontend) — Done
- Scaffold Next.js frontend (`apps/web`) + set search page — Done (MUI, TanStack Query, RHF+Zod, Vitest)

Success criteria: user can search a LEGO set and view its parts inventory. Backend met; frontend search shipped, set-detail/parts-inventory UI deferred to next slice.

## Phase 2 — User Collection

Status: In Progress

- Auth foundation (2a) — Done: JWT (stateless), users table, register/login/me, Spring Security
- Add set to collection (2b) — Done: `user_sets` table (V6), `POST/GET/PATCH/DELETE /api/v1/collection/sets`, auth + owner-scoped, find-or-import target set, status/purchase price+date, 409 on duplicate, partial update + remove (cross-user → 404)
- Loose pieces manual inventory (2c) — Done: `user_parts` table (V7), `POST/GET/PATCH/DELETE /api/v1/collection/parts`, quantity by part+color, storage location, unique per (user, part, color) + 409, owner-scoped; part+color must be pre-imported in catalog (missing ref → 404)
- Frontend login/register wiring — Done: auth client, login/register pages, protected routes, nav/logout, collection UI (owned sets + loose parts)
- Frontend collection pagination — Done: shared `PaginationControls` (prev/next, hidden on single page) on both collection lists; `page` state per section drives the paginated hooks

Frontend collection edit (PATCH) — Done: per-row Edit buttons open `EditSetDialog` (status/price/date) and `EditPartDialog` (quantity/storage), both RHF+Zod; empty optional fields omitted (backend cannot clear to null). All deferred Phase 2 collection-edit polish complete.

Decomposed into 2a (auth) → 2b (add-set) → 2c (loose pieces). Auth = JWT stateless (see `docs/superpowers/specs/2026-07-06-auth-foundation-design.md`).

## Phase 3 — Missing Pieces Engine

Status: Done (core)

- 3a backend engine + endpoint — Done: `GET /api/v1/sets/{setNumber}/missing-parts` (authenticated) compares a target set's required (non-spare) inventory vs the user's owned inventory (loose parts + parts of owned/built/in-progress sets); returns per part+color required/owned/missing plus completion percentage. 404 if set or inventory not imported.
- 3c frontend — Done: "Missing pieces" panel on the set-detail page (`/sets/[setNumber]`) — completion bar + per-part required/owned/missing table; auth-gated (prompts login when signed out; prompts inventory import on 404).
- 3b richer report — Done: `missingOnly` filter + line pagination (`page`/`size`, `totalPages`, `first`/`last`), `missingLineCount`; whole-set totals unchanged. Frontend: "Only missing" toggle + prev/next paging. (Configurable spare policy dropped — fixed documented rule.)

Success criterion met: a user can see exactly how close they are to completing a set. Owned = loose parts (`user_parts`) + parts of collection sets with status OWNED/BUILT/IN_PROGRESS (WISHLIST excluded); spares count toward owned but not toward required.

## Phase 4 — Set Comparison Engine

Status: Done (core)

- Backend engine + endpoint — Done: `GET /api/v1/sets/compare?a=&b=&category=&page=&size=` (public) compares two catalog sets' non-spare inventories; returns a quantity-weighted similarity score (`sum(min)/sum(max)`, 2 dp), per part+color diff lines (`quantityA`/`quantityB`/`shared`/`category` ONLY_A|ONLY_B|BOTH), whole-set line counts, and paginated lines with an optional category filter. 404 if either set or its inventory is not imported.
- Frontend compare page — Done: `/compare?a=&b=` (URL-driven, shareable) — side-by-side A-vs-B header, quantity-weighted similarity meter, category filter (All/Both/Only A/Only B), diff table (part/color/qty A/qty B/shared/category chip), pagination (reuses `PaginationControls`), loading/404/error/empty states. Slices: types+api → `useSetComparison` hook → `SetComparisonView` + route (TDD). PR #33 → develop.
- Later: inventory overlap visualization, metadata diff; `Compare` nav link for discoverability.

Success criterion met: a user can visually compare two sets' parts and see how similar they are.

## Phase 5 — Build Recommendation Engine

Status: Done (core)

- Backend engine + endpoint (slice 1) — Done: `GET /api/v1/recommendations/buildable?buildableOnly=&page=&size=` (authenticated) scores the user's WISHLIST sets by how buildable they are from owned inventory (loose parts + parts of OWNED/BUILT/IN_PROGRESS sets), most-complete first; skips wishlist sets with no imported inventory; `buildableOnly` filter; `PageResponse<BuildableSetRecommendation>`. Extracted shared `OwnedInventoryService` (owned-map + `PartColorKey`) reused by the missing-pieces engine.
- Frontend recommendations page (slice 2) — Done: protected `/recommendations` (RequireAuth) — per-set completion bar + Buildable/Incomplete badge + owned/required + missing, `Buildable only` toggle, pagination. Nav link (authenticated). Mirrors the missing-pieces panel idiom.
- Later: widen candidate scope beyond wishlist (whole catalog with batching/limits); almost-buildable thresholds.

Success criterion met: a user can see which of their wishlist sets they can build (or are closest to building) from what they own.

## Phase 6 — Price Tracking and Deals

Status: In Progress (backend slice 1 done)

- Decision — Done: ADR-011 (user-submitted snapshots first, BrickLink next; scraping excluded). Spike + slice-1 TDD in `docs/superpowers/specs/`.
- Backend slice 1 (manual snapshots + deal detection) — Done: `price_snapshots` table (V8, source-agnostic), `pricing` package. Authenticated, owner-scoped `POST/GET/DELETE /api/v1/price-snapshots` + `GET /api/v1/sets/{setNumber}/price-analysis?currency=&candidatePrice=` → min/avg/max/latest + price-per-piece + deal verdict (GREAT_DEAL/GOOD_DEAL/FAIR/POOR). Per-currency; strict 404 when no snapshots. Shared `PriceAnalysisService` is pure.
- Next: frontend price-tracking UI (add snapshot + analysis panel); later BrickLink adapter (POC-gated) + wishlist price alerts.

## Phase 7 — AI-Assisted Classification

Status: Not Started

- Photo → part/color suggestion + confidence + user confirm

## Phase 8 — Productization

Status: Not Started

- Subscriptions, limits, premium AI, notifications, deployment, legal
