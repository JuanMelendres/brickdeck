# Feature: Collection — Owned Sets

## Summary
Authenticated users add LEGO sets to a personal collection, tracking status,
purchase price, and purchase date, and can update or remove entries.

## Problem
Collectors need a digital record of the sets they own (or want), including how
much they paid and the set's current status in their collection.

## Users
Authenticated users only.

## Functional Requirements
- FR-001: `POST /api/v1/collection/sets` adds a set (find-or-import target) and returns `201` + `Location`.
- FR-002: `GET /api/v1/collection/sets` lists the user's sets as `PageResponse<UserSetResponse>` (default `size=20`, `sort=createdAt DESC`).
- FR-003: `PATCH /api/v1/collection/sets/{id}` partially updates status / purchasePrice / purchaseDate.
- FR-004: `DELETE /api/v1/collection/sets/{id}` removes an entry (`204`).

## Business Rules
- BR-001: A user cannot add the same set twice — unique `(user_id, set_id)`; duplicate returns `409`.
- BR-002: All operations are owner-scoped; another user's entry is treated as not found (`404`, no existence leak).
- BR-003: The target set is find-or-imported from the catalog if not already cached.
- BR-004: Partial update cannot clear a field to null; only non-null fields are applied.
- BR-005: Status is one of `OWNED`, `WISHLIST`, `BUILT`, `IN_PROGRESS`.

## User Flow
1. User finds a set via search.
2. User adds it to their collection with a status and optional purchase details.
3. User lists, updates, or removes collection entries.

## Edge Cases
- Adding a duplicate: `409`.
- Updating/deleting a set owned by another user: `404`.
- Invalid status or negative price: `400`.

## Out of Scope
- Quantity per set (a set is present or not).
- Condition, store, and sold/for-sale statuses (future scope).

## Open Questions
- Should removing a set be a soft delete (archive) instead of a hard delete?
