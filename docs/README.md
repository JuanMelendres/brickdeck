# BrickDeck Documentation

Docs-as-code for BrickDeck. Start here.

## Product
- [Vision](./product/vision.md) — what BrickDeck is, the problem, users, scope.
- [Roadmap](./product/roadmap.md) — phased plan.
- [Features](./product/features.md) — feature list with status.
- [FDDs](./product/fdd/) — [set search](./product/fdd/set-search-fdd.md), [set inventory](./product/fdd/set-inventory-fdd.md), [authentication](./product/fdd/authentication-fdd.md), [collection sets](./product/fdd/collection-sets-fdd.md), [collection loose parts](./product/fdd/collection-loose-parts-fdd.md), [missing pieces](./product/fdd/missing-pieces-fdd.md).
- [AI strategy](./product/ai-strategy.md) · [Pricing & scraping policy](./product/pricing-scraping-policy.md)

## Architecture
- [Overview](./architecture/overview.md) — system context, layout, data flow.
- [Technical design](./architecture/technical-design.md) — catalog import & collection core.
- [Database design](./architecture/database-design.md) — schema & migrations.
- [API design](./architecture/api-design.md) — endpoints, conventions, errors.
- [Diagrams](./architecture/diagrams.md) — Mermaid index.

## Decisions (ADRs)
- [ADR-001 Modular monorepo](./decisions/ADR-001-modular-monorepo.md)
- [ADR-002 Java 21 + Spring Boot](./decisions/ADR-002-java-21-spring-boot.md)
- [ADR-003 PostgreSQL + Flyway](./decisions/ADR-003-postgresql-flyway.md)
- [ADR-004 Docker Compose local dev](./decisions/ADR-004-docker-compose-local-dev.md)
- [ADR-005 Rebrickable first](./decisions/ADR-005-rebrickable-first.md)
- [ADR-006 Defer scraping & AI](./decisions/ADR-006-defer-scraping-and-ai.md)
- [ADR-007 MUI frontend](./decisions/ADR-007-mui-frontend.md)
- [ADR-008 Stateless JWT auth](./decisions/ADR-008-jwt-stateless-auth.md)
- [ADR-009 Hexagonal + DDD packages](./decisions/ADR-009-hexagonal-ddd-packages.md)
- [ADR-010 Git-flow / develop default](./decisions/ADR-010-git-flow-develop-default.md)
- [ADR-011 Price data sourcing](./decisions/ADR-011-price-data-sourcing.md)

## API
- [API docs & conventions](./api/README.md)
- [openapi.yaml](./api/openapi.yaml) · [Postman](./api/postman/)

## Testing
- [Strategy](./testing/testing-strategy.md) · [Test plan](./testing/test-plan.md) · [Unit](./testing/unit-testing.md) · [Integration](./testing/integration-testing.md)

## Development
- [Setup](./development/setup.md) · [Environment variables](./development/environment-variables.md) · [Coding standards](./development/coding-standards.md) · [Contribution guide](./development/contribution-guide.md)

## Design specs & plans
- [`superpowers/specs`](./superpowers/specs/) and [`superpowers/plans`](./superpowers/plans/) — per-feature SDD artifacts.
