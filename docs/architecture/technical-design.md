# Technical Design: Catalog Import & Collection Core

This TDD documents the two implemented backend subsystems: catalog find-or-import
(sets + inventory) and the user collection (sets + loose parts). New feature-level
designs live under [`docs/superpowers/specs`](../superpowers/specs/); this is the
consolidated, current-state view.

## Context
BrickDeck must present LEGO catalog data without re-fetching it on every request,
and must let authenticated users track owned sets and loose pieces. Rebrickable is
the upstream catalog source; it has rate limits and cannot be a per-request
dependency.

## Goals
- Serve catalog reads from a local cache; call Rebrickable only on cache misses.
- Import set inventories idempotently.
- Isolate per-user collection data with strict owner scoping.
- Never expose entities or raw external payloads.

## Non-Goals
- Full catalog pre-sync / background jobs.
- Comparison, recommendation, and pricing engines.
- Single-part import directly from Rebrickable.

## Proposed Solution (implemented)

### Find-or-import (cache-first)
Lookups resolve by canonical set number. On a local hit the service returns the
cached `BrickSetResponse` (`cacheStatus = LOCAL_CACHE_HIT`) and does **not** call
Rebrickable or save. On a miss it fetches, normalizes, upserts, and returns
`IMPORTED_FROM_REBRICKABLE`.

### Inventory import
`SetInventoryService.importInventory(setNumber)` requires the set to exist locally,
fetches all Rebrickable pages, upserts shared `Color`/`Part` reference data, then
upserts `set_parts` lines idempotently (find-by set/part/color/spare before insert).

### Collection
`CollectionService` / `UserPartService` operate owner-scoped via
`findByIdAndUserId`. Adds are dedup-checked (`existsBy...`) and find-or-import the
target set when needed. Loose parts resolve part+color from the local catalog only.

## Architecture
Controllers → services → repositories (Spring Data JPA). External access is
isolated in `external.rebrickable`. Mapping to DTO records happens in the service
/ DTO factory layer. See [overview.md](./overview.md).

## Data Model
See [database-design.md](./database-design.md). Key invariants:
`set_parts` unique `(set_id, part_id, color_id, is_spare)`;
`user_sets` unique `(user_id, set_id)`; `user_parts` unique `(user_id, part_id, color_id)`.

## API Design
See [api-design.md](./api-design.md).

## Validation Rules
- Bean Validation on request records (`@NotBlank`, `@Email`, `@Size`, `@Positive`, etc.).
- Set-number normalization: bare number to default variant (`42232` to `42232-1`).
- Collection status constrained to the `CollectionStatus` enum.

## Error Handling
Centralized in `GlobalExceptionHandler`: `400` validation, `401` auth,
`404` not-found/not-owned, `409` duplicate. No existence leak across users
(not-owned reads as `404`).

## Testing Strategy
Service (business logic), controller (`@WebMvcTest` + MockMvc, `@MockitoBean`),
repository (custom queries), and integration (`@SpringBootTest` + Testcontainers).
Cover find-or-import both ways. See [../testing/testing-strategy.md](../testing/testing-strategy.md).

## Security Considerations
- Passwords BCrypt-hashed; JWT HS256 with an env-provided secret (>= 32 bytes).
- Collection endpoints require a valid Bearer token; all queries are owner-scoped.
- Rebrickable API key is env-only, never committed.
- CORS restricted to configured origins.

## Observability
- Spring Actuator `health` / `info` exposed.
- **TODO:** structured request logging, correlation IDs, and metrics are not yet
  configured.

## Alternatives Considered
- **Full catalog pre-sync:** rejected for MVP (API limits, storage, licensing);
  on-demand import is simpler and sufficient.
- **Session-based auth:** rejected in favor of stateless JWT for a separate SPA
  frontend ([ADR-008](../decisions/ADR-008-jwt-stateless-auth.md)).

## Open Questions
- Auto-import a set when inventory import is requested for an uncached set?
- Add single-part find-or-import for loose pieces?
