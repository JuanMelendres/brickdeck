# Feature: Missing Pieces Engine

## Summary
For a target set, tell the user exactly how close they are to completing it —
which parts they still need, which they already have, and an overall completion
percentage.

## Problem
A collector wants to build a set but may already own many of its parts (loose, or
inside other sets they own). They need to know precisely what is still missing
before buying.

## Users
Authenticated users.

## Functional Requirements
- FR-001: `GET /api/v1/sets/{setNumber}/missing-parts` (authenticated) returns a `MissingPartsReport`.
- FR-002: For each required part+color: `required`, `owned`, `missing`.
- FR-003: Report includes `totalRequired`, `totalOwned`, `totalMissing`, and `completionPercentage`.
- FR-004: `404` when the target set or its inventory is not imported.

## Business Rules
- BR-001: Required = the target set's **non-spare** inventory lines.
- BR-002: Owned = loose parts (`user_parts`) **plus** the parts of the user's sets whose status is `OWNED`, `BUILT`, or `IN_PROGRESS` (`WISHLIST` excluded).
- BR-003: Spare parts count toward owned, never toward required.
- BR-004: Owned is matched by internal part id + color id.
- BR-005: `completionPercentage` = share of required pieces owned, capped per line (owning extra does not exceed 100 percent), rounded to one decimal.

## User Flow
1. User opens a target set.
2. User requests the missing-pieces report.
3. The report lists each part with required/owned/missing and an overall completion percentage.

## Edge Cases
- Set not imported: `404`.
- Inventory not imported (no non-spare lines): `404`.
- User owns more than required for a part: `missing` is 0; excess does not inflate completion.
- User owns nothing: completion 0 percent, all parts missing.

## Out of Scope
- Filtering to only-missing lines and pagination (planned: 3b).
- Frontend "Missing pieces" view (planned: 3c).
- Suggesting where to buy missing parts.

## Open Questions
- Should quantity beyond a single copy of the set (building two of a set) be supported?
- Should spare requirements ever be surfaced separately?
