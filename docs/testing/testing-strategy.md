# Testing Strategy

Testing is TDD-first: write a failing test (RED), then minimal code to pass
(GREEN), then refactor. Every meaningful change adds or updates tests.

## Backend (`apps/api`)

Stack: JUnit 5, Mockito, Spring MockMvc, Testcontainers, Flyway.

| Layer | Tool | What it covers |
| --- | --- | --- |
| Service | JUnit 5 + Mockito | Business logic in isolation (mock repositories/clients). |
| Controller | `@WebMvcTest` + MockMvc | HTTP status, routing, validation, serialization. Mock services with `@MockitoBean`. |
| Repository | `@DataJpaTest` / slice | Custom queries and constraints. |
| Integration | `@SpringBootTest` + Testcontainers | End-to-end workflows against real PostgreSQL. |

Conventions:
- Use `@MockitoBean`, **not** the deprecated `@MockBean` (Spring Boot 3.5.x).
- Catalog `@WebMvcTest` slices use `@AutoConfigureMockMvc(addFilters=false)`;
  collection/auth controller tests keep the security filters and authenticate via
  MockMvc post-processors.
- Arrange–Act–Assert structure.
- For paginated bodies assert `$.content`, `$.content[0].id`, `$.page` — never `$[0]`.
- Cover find-or-import both ways: `LOCAL_CACHE_HIT` and `IMPORTED_FROM_REBRICKABLE`.

## Frontend (`apps/web`)

Stack: Vitest + React Testing Library + jsdom.

- Test behavior, not implementation. Prefer accessible queries.
- Mock the API-client layer / hooks; wrap query hooks in a `QueryClientProvider`.
- Handle loading, error, and empty states in tests.

## Current Coverage (snapshot)

- Backend: ~23 test classes across service, controller, repository, and integration
  layers (auth, catalog, inventory, collection sets + parts, CORS, OpenAPI).
- Frontend: ~11 test files covering the API client, hooks, and set search/detail UI.

See [test-plan.md](./test-plan.md) for what is and isn't covered, and
[unit-testing.md](./unit-testing.md) / [integration-testing.md](./integration-testing.md)
for how to run each kind.

## What's Missing (TODO)

- No CI pipeline yet — tests run locally only. Add GitHub Actions to run
  `./mvnw verify` (with a PostgreSQL service) and `npm run test`/`typecheck`/`lint`.
- No coverage reporting / SonarCloud gate configured.
- Frontend has no auth or collection UI yet, so those flows are untested end-to-end.

## Recommendations (keep it lightweight)

- Add CI first — it protects everything else.
- Add coverage reporting but avoid hard percentage gates early; watch trends.
- Keep integration tests focused on real workflows; don't duplicate unit coverage.
