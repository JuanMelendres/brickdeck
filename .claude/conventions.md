# BrickDeck Engineering Conventions

## Java

- Use Java 21.
- Prefer records for DTOs and request/filter objects (e.g. `BrickSetResponse`, `ImportSetRequest`, `ImportResult`).
- Keep controllers thin.
- Keep services focused on business logic.
- Keep repositories focused on persistence.
- Never expose entities or raw third-party (Rebrickable) responses through controllers.
- Use Bean Validation for request validation.
- Use `PageResponse<T>` for paginated collection responses when list endpoints are added.

## Spring Boot

- Use constructor injection.
- Prefer `@RequiredArgsConstructor`.
- Use `@RestControllerAdvice` (`GlobalExceptionHandler`) for global errors.
- Throw `ResourceNotFoundException` (in `common`) for missing resources; it maps to 404 `{message}`.
- Use `ResponseEntity` for status control.
- Use `@Valid` on request bodies.
- Use `@PageableDefault` on collection GET endpoints when pagination is introduced.

## Architecture

- DDD + hexagonal: keep core catalog logic separate from infrastructure (Rebrickable client, JPA).
- Organize by domain: `catalog`, `health`, `common`, `external.rebrickable`.
- Keep external API clients isolated in `external.*` integration packages.
- Normalize external catalog data into internal entities; cache locally (find-or-import, local hit skips Rebrickable).
- Rebrickable is the primary catalog source; keys via environment variables.

## External API (Rebrickable)

- Connect/read timeouts on the client.
- Retries and rate-limit awareness for external calls.
- Cache imported data locally; do not re-fetch on a local cache hit.
- `cacheStatus` on responses: `LOCAL_CACHE_HIT` | `IMPORTED_FROM_REBRICKABLE`.

## Database

- Flyway migrations under `apps/api/src/main/resources/db/migration`.
- Do not modify applied migrations after commit.
- Unique constraints for set numbers, part numbers, theme external IDs.
- Track external source and sync timestamp on imported data.
- Entities: ensure `@PrePersist`/`@CreationTimestamp` for non-null `created_at`/`updated_at`.

## Testing

- Use JUnit 5.
- Use Mockito for service/controller unit tests.
- Use `@MockitoBean` (Spring Boot 3.5.x) in `@WebMvcTest`, not deprecated `@MockBean`.
- Use MockMvc for controller tests.
- Use Testcontainers for integration/repository tests.
- Full `@SpringBootTest` needs Postgres on `localhost:5433`; pure slices avoid the DB.
- Test validation error body, not only status code.
- Use unique test values when constraints are unique.

## API

- POST returns 201 Created.
- DELETE returns 204 No Content.
- GET/PUT/PATCH return 200 OK.
- Detail endpoints return a DTO record directly.
- Collection endpoints return `PageResponse` (when added).
- Error responses are standardized via `GlobalExceptionHandler`.

## Git

Always use Conventional Commits.

Allowed prefixes:

- feat
- fix
- test
- refactor
- docs
- ci
- chore
- perf
- style

Examples:

- `feat(catalog): add set import endpoint`
- `fix(catalog): add connect/read timeouts to Rebrickable client`
- `test(catalog): add set import upsert coverage`
- `refactor(catalog): extract theme resolution helper`
