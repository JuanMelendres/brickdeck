# Feature: Set Search

## Summary
Search the LEGO catalog by free-text query and browse paginated results.

## Problem
Users need to find a specific LEGO set before they can view its inventory or add
it to their collection. The full catalog is too large to browse manually.

## Users
Any user (search is public — no authentication required).

## Functional Requirements
- FR-001: `GET /api/v1/sets/search?q=&page=&size=` returns matching sets.
- FR-002: Results are paginated with a `PageResponse<BrickSetResponse>` envelope (0-indexed).
- FR-003: Search delegates to Rebrickable fuzzy search; local pages are 0-indexed and converted to Rebrickable's 1-indexed pages.
- FR-004: Each result carries a `cacheStatus` of `EXTERNAL_SEARCH_RESULT`.

## Business Rules
- BR-001: Search results are not persisted; they are external lookups.
- BR-002: A set is only cached locally when explicitly fetched/imported (find-or-import), not during search.

## User Flow
1. User types a query into the search bar (`/sets` page).
2. Frontend calls the search endpoint with `q`, `page`, `size`.
3. Results render as cards; user paginates with prev/next.
4. Clicking a set opens its detail page.

## Edge Cases
- Empty query: TODO — confirm backend behavior (likely empty/handled by Rebrickable).
- No results: frontend shows an empty state.
- Rebrickable timeout/error: surfaced via `GlobalExceptionHandler`.

## Out of Scope
- Filtering by theme/year/price.
- Local full-text search over cached sets.

## Open Questions
- Should very short queries be rejected client-side to reduce API calls?
