# Unit Testing

Fast tests that do not need a database or network.

## Backend

Unit/slice tests cover services (Mockito) and controllers (`@WebMvcTest`). These
do **not** require PostgreSQL.

Run a focused test class:

```bash
cd apps/api
./mvnw -Dtest=BrickSetServiceTest test
```

Parse noisy Maven output:

```bash
./mvnw -Dtest=BrickSetServiceTest test 2>&1 | grep -E "Tests run:|BUILD"
```

What belongs here:
- Service business logic (find-or-import branching, idempotent upserts, owner scoping).
- Controller HTTP behavior: status codes, validation (`400` + `validationErrors`),
  serialization, routing. Mock services with `@MockitoBean`.
- Mapping/normalization from Rebrickable DTOs to internal models.
- `JwtService` token creation/validation.

What does **not** belong here: real DB queries, real HTTP to Rebrickable, full
application context wiring — those are integration tests.

## Frontend

```bash
cd apps/web
npm run test          # vitest run (once)
npm run test:watch    # watch mode
```

What belongs here:
- API client (`apiGet`/`apiPost`) request shaping and error handling.
- Hooks (TanStack Query) wrapped in a `QueryClientProvider`, with the client layer mocked.
- Components: rendering of loading/error/empty/success states via accessible queries.

## Naming

- Backend: `<ClassUnderTest>Test.java` (e.g. `CollectionServiceTest`).
- Frontend: `<module>.test.ts(x)` colocated with the code under test.
