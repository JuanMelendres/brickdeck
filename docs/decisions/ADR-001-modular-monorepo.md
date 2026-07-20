# ADR-001: Modular Monorepo

## Status
Accepted

## Date
2026-07-02

## Context
BrickDeck spans a backend API, a web frontend, and a possible future AI service.
These need to evolve together, share documentation, and be easy to run locally by
a single developer (and AI assistants).

## Decision
Use a single monorepo. Applications live under `apps/` (`apps/api`, `apps/web`).
`services/`, `packages/`, and `infra/` are reserved as placeholders for future
growth (e.g. an AI service, shared packages, deployment scripts).

## Consequences
- Positive: one clone, shared docs, atomic cross-cutting changes, simple local setup.
- Positive: AI tools get full project context in one place.
- Negative: no independent versioning/release per app (acceptable at this stage).
- Note: original docs proposed `services/api`; the backend actually lives in
  `apps/api`. The reserved top-level folders are currently empty.

## Alternatives Considered
- Separate repos per service: more overhead, harder local dev, weaker AI context.

## Notes
Layout is documented in [architecture/overview.md](../architecture/overview.md).
