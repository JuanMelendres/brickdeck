# ADR-009: Hexagonal + DDD Package Structure

## Status
Accepted

## Date
2026-07-03

## Context
The backend integrates an external API, persists normalized data, and exposes REST
endpoints. Without discipline, third-party models and persistence concerns tend to
leak into the domain and the API surface.

## Decision
Organize code by business domain (DDD) with hexagonal separation between core logic
and infrastructure. Base package `com.brickdeck.api`, split into `catalog`,
`collection`, `security`, `external.rebrickable`, `health`, and `common`. Rules:
- External clients stay isolated in `external.*`.
- Entities are never returned from controllers — map to DTO records.
- Raw third-party responses are normalized into internal entities before use.

## Consequences
- Positive: domain stays pure; swapping/adding an external source is localized.
- Positive: clear ownership per domain; easier testing per layer.
- Negative: more mapping code (DTOs, normalization) — an intentional trade-off.

## Alternatives Considered
- Layer-by-technical-type packaging (`controllers/`, `services/`, `entities/`):
  rejected — scatters a feature across packages and invites leaks.

## Notes
See [architecture/overview.md — Backend Architecture](../architecture/overview.md#backend-architecture).
