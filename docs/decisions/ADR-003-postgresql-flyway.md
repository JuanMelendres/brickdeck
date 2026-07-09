# ADR-003: PostgreSQL + Flyway for Persistence

## Status
Accepted

## Date
2026-07-02

## Context
Catalog and collection data is relational (sets, parts, colors, themes, users,
and their join tables) with strong uniqueness and referential-integrity needs.
Schema changes must be versioned and reproducible across environments.

## Decision
Use PostgreSQL 16 as the database and Flyway for versioned migrations. Hibernate
runs with `ddl-auto: validate` so migrations are the single source of truth for
schema. Local PostgreSQL runs via Docker Compose on host port `5433`.

## Consequences
- Positive: reliable constraints (unique keys, FKs), predictable migrations, easy
  local reset via Docker volume.
- Positive: `validate` prevents silent schema drift.
- Negative: integration tests need a running PostgreSQL (mitigated by Testcontainers).
- Note: host port is `5433` (not the default `5432`) to avoid clashing with a local
  PostgreSQL.

## Alternatives Considered
- Hibernate `ddl-auto: update`: rejected — non-deterministic, unsafe for production.
- MySQL/SQLite: PostgreSQL preferred for constraints, types, and JSON support.

## Notes
Migrations: `apps/api/src/main/resources/db/migration` (`V1..V7`). See
[architecture/database-design.md](../architecture/database-design.md).
