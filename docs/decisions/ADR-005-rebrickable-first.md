# ADR-005: Rebrickable as the First Catalog Source

## Status
Accepted

## Date
2026-07-02

## Context
BrickDeck needs structured LEGO data (sets, parts, colors, inventories, themes)
rather than building a catalog by hand. Several providers exist (Rebrickable,
BrickLink, Brickset).

## Decision
Integrate Rebrickable first as the primary catalog source. Keep the client isolated
in `external.rebrickable` with connect/read timeouts. Normalize responses into
internal entities and cache locally; reads are cache-first (find-or-import). Track
`source` and sync timestamps on imported data. The API key is provided via
`REBRICKABLE_API_KEY` and never committed.

## Consequences
- Positive: rich, structured data covering MVP needs (search, details, inventories).
- Positive: local caching decouples user requests from upstream availability/limits.
- Negative: single upstream dependency; rate limits affect imports (mitigated by cache).
- Negative: data model must be mapped/normalized (never expose raw payloads).

## Alternatives Considered
- BrickLink: better for marketplace prices; deferred to a later phase.
- Brickset: additional metadata; deferred.
- Manual catalog entry: not viable at scale.

## Notes
See [architecture/overview.md — External Integrations](../architecture/overview.md#external-integrations).
