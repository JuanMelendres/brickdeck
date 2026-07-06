---
name: backend-spring-boot
description: Use this skill when modifying the BrickDeck Spring Boot backend, including controllers, services, repositories, DTOs, the Rebrickable client, catalog import/lookup, validation, Flyway migrations, or API behavior.
---

# Backend Spring Boot Skill

## Role

Act as a senior Java 21 and Spring Boot 3 backend engineer.

## Project Context

This project is BrickDeck, a Spring Boot REST API (`apps/api`, base package `com.brickdeck.api`) for a LEGO collection intelligence platform. Catalog data is imported and cached from Rebrickable.

Core packages:
- `catalog` (`controller`, `service`, `repository`, `entity`, `dto`)
- `external.rebrickable` (`client`, `config`, `dto`)
- `common` (`GlobalExceptionHandler`, `ResourceNotFoundException`)
- `health`

Testing stack:
- JUnit 5
- Mockito (`@MockitoBean` for `@WebMvcTest`)
- MockMvc
- Testcontainers

## Rules

When changing backend code:

1. Inspect existing style first.
2. Keep changes small and focused.
3. Preserve existing architecture (DDD + hexagonal; keep the Rebrickable client isolated in `external`).
4. Do not introduce unrelated refactors.
5. Follow TDD: write a failing test, watch RED, then minimal GREEN.
6. Update tests when behavior changes.
7. Use DTO records, not entities, in controllers. Never expose raw Rebrickable responses.
8. Normalize external data into entities; keep find-or-import cache-first (local hit skips Rebrickable and skips save).
9. Use Bean Validation in request records.
10. Throw `ResourceNotFoundException` for missing resources; let `GlobalExceptionHandler` map errors.
11. Add `@PrePersist`/`@CreationTimestamp` before persisting entities with non-null timestamps.
12. Use `PageResponse<T>` + `@PageableDefault` when introducing collection GET endpoints.
13. Use Conventional Commits.

## API Response Rules

Detail GET returns a DTO record (e.g. `BrickSetResponse` with `externalThemeId`, `externalUrl`, `cacheStatus`).

Collection GET returns `PageResponse<T>` (when pagination is added).

POST returns `ResponseEntity.created(location).body(response)`.

DELETE returns `ResponseEntity.noContent().build()`.

## External API Rules

- Rebrickable is the primary source; keys via environment variables.
- Keep connect/read timeouts on the client; add retries and respect rate limits.
- Cache imported data locally.

## Before Finishing

Suggest the exact tests to run (from `apps/api`; append `.cmd` to `./mvnw` only on Windows CMD):

```bash
nc -z -w2 localhost 5433   # integration tests need Postgres
./mvnw -Dtest=RelevantTest test
./mvnw clean verify
```

End with a Conventional Commit suggestion.
