# Feature: Collection — Loose Parts

## Summary
Authenticated users manually track loose LEGO pieces by part and color, with a
quantity and an optional storage location.

## Problem
Collectors accumulate loose pieces outside of complete sets. They need to record
what they have (part, color, quantity, where it's stored) to power future
missing-piece and build-recommendation features.

## Users
Authenticated users only.

## Functional Requirements
- FR-001: `POST /api/v1/collection/parts` adds a loose part entry and returns `201` + `Location`.
- FR-002: `GET /api/v1/collection/parts` lists entries as `PageResponse<UserPartResponse>` (default `size=20`, `sort=createdAt DESC`).
- FR-003: `PATCH /api/v1/collection/parts/{id}` updates quantity / storage location.
- FR-004: `DELETE /api/v1/collection/parts/{id}` removes an entry (`204`).

## Business Rules
- BR-001: Part and color are resolved from the local catalog by `externalPartNumber` / color `externalId`.
- BR-002: The part and color must already be cached (imported via some set's inventory); otherwise `404`.
- BR-003: Unique `(user_id, part_id, color_id)`; duplicate returns `409`.
- BR-004: All operations are owner-scoped (`404` for another user's entry).
- BR-005: Quantity must be positive.

## User Flow
1. User identifies a loose piece (part number + color).
2. User records quantity and optional storage location.
3. User lists, updates, or removes entries.

## Edge Cases
- Part/color not in catalog: `404` (no single-part Rebrickable fetch exists yet).
- Duplicate part+color for the same user: `409` (update the existing entry instead).
- Non-positive quantity: `400`.

## Out of Scope
- AI image recognition to identify pieces.
- Single-part find-or-import directly from Rebrickable (planned; see roadmap).
- Condition and source-set tracking.

## Open Questions
- Add single-part find-or-import so pieces can be added without pre-importing a set?
