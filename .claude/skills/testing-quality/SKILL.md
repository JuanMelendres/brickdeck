---
name: testing-quality
description: Use this skill when adding, updating, debugging, or reviewing tests, coverage, GitHub Actions, or integration tests in BrickDeck.
---

# Testing and Quality Skill

## Role

Act as a senior QA-minded backend engineer with strong experience in JUnit 5, Mockito, MockMvc, Testcontainers, and CI pipelines.

## Testing Strategy

Use the correct test type:

- Service tests for business logic (catalog import/upsert, theme resolution)
- Controller tests (`@WebMvcTest`) for HTTP status, validation, response shape, error body — mock services with `@MockitoBean`
- Repository tests for custom query methods
- Integration tests (`@SpringBootTest` + Testcontainers) for full workflows

## Execution Rules

- Run Maven from `apps/api` via `./mvnw`.
- Full `mvnw test` / `@SpringBootTest` need Postgres on `localhost:5433` (`ddl-auto=validate` + Flyway). Check first: `nc -z -w2 localhost 5433`.
- Pure unit / `@WebMvcTest` slices do not need the DB: `./mvnw -Dtest=ClassA,ClassB test`.
- Maven output is noisy (Mockito self-attach, JDK agent, CDS warnings are benign). Parse with `./mvnw ... 2>&1 | grep -E "Tests run:|BUILD"`.

## Paginated API Test Rules (when list endpoints exist)

Assert on the page envelope, not a raw array:

```java
jsonPath("$.content")
jsonPath("$.content[0].id")
jsonPath("$.page")
jsonPath("$.size")
jsonPath("$.totalElements")
jsonPath("$.totalPages")
```

Never assert `jsonPath("$[0]")` unless the endpoint truly returns a raw array.

## Integration Test Rules

- Use explicit pagination (page=0, size=10, sort=id,asc) once paginated.
- Do not assume the database contains only one record.
- Use unique names/numbers for entities with unique constraints.
- Cover find-or-import both ways: `LOCAL_CACHE_HIT` and `IMPORTED_FROM_REBRICKABLE`.

## Validation Test Rules

For invalid requests, assert the standardized error body from `GlobalExceptionHandler` (status, error, message, and field errors). Test the body, not only the status code.

## Commands

```bash
./mvnw -Dtest=ClassName test   # focused, no DB for slices
./mvnw clean verify            # full, needs Postgres on 5433
```

## Completion Criteria

A task is complete only when:

- Code compiles
- Relevant tests pass (RED → GREEN via TDD)
- Full verify is expected to pass
- A Conventional Commit message is provided
