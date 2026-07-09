# Feature: Set Parts Inventory

## Summary
Import and view the full parts inventory of a LEGO set — parts, colors, and
quantities — sourced from Rebrickable and cached locally.

## Problem
Collectors need to know exactly which parts (and how many, in which colors) a set
contains, both to understand what they own and to power future missing-piece and
build-recommendation features.

## Users
Any user (read is public; import is triggered on demand).

## Functional Requirements
- FR-001: `POST /api/v1/catalog/sets/{setNumber}/inventory/import` imports the inventory; the set must already exist locally.
- FR-002: `GET /api/v1/sets/{setNumber}/parts` returns `PageResponse<SetPartResponse>` (default `size=50`, `sort=id`).
- FR-003: Import fetches all Rebrickable pages and upserts colors, parts, and `set_parts` lines idempotently.
- FR-004: `InventoryImportResult` reports `setNumber` and `linesProcessed`.

## Business Rules
- BR-001: A `set_parts` line is unique on `(set_id, part_id, color_id, is_spare)`.
- BR-002: Import is idempotent — re-running does not duplicate lines.
- BR-003: Colors and parts are shared reference data (upserted, not duplicated per set).
- BR-004: Bare set numbers (`42232`) normalize to the default variant (`42232-1`).

## User Flow
1. User opens a set detail page.
2. If no inventory is cached, the UI shows an "Import inventory" action.
3. Import runs; parts render in a paginated table with color and quantity.
4. Subsequent views read from the local cache.

## Edge Cases
- Set not imported yet: import returns 404 (import the set first).
- Spare parts: tracked separately via `is_spare`.
- Large inventories: paginated (`size=50` default).

## Out of Scope
- Minifigure inventories.
- Alternate builds / MOC inventories.

## Open Questions
- Should inventory import auto-trigger a set import when the set is missing?
