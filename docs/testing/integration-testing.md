# Integration Testing

Tests that exercise real workflows against real infrastructure (PostgreSQL via
Testcontainers, full Spring context).

## Prerequisites

Integration tests and the full `./mvnw verify` need PostgreSQL reachable on
`localhost:5433`. Check first:

```bash
nc -z -w2 localhost 5433 && echo "db up" || echo "start docker compose"
```

Start it if needed:

```bash
docker compose up -d
```

> Testcontainers may spin up its own PostgreSQL container for `@SpringBootTest`
> integration tests; the Docker daemon must be running.

## Running

```bash
cd apps/api
./mvnw verify                                   # full build + all tests
./mvnw -Dtest=CollectionIntegrationTest test    # a single integration test
./mvnw verify 2>&1 | grep -E "Tests run:|BUILD" # parse noisy output
```

## What belongs here

- Auth end-to-end (`AuthIntegrationTest`): register to login to `me`.
- Collection workflows (`CollectionIntegrationTest`, `UserPartIntegrationTest`):
  add/list/update/delete, duplicate `409`, owner-scoping `404`.
- Repository constraint behavior against real PostgreSQL.

## Conventions

- The shared integration DB may already contain real records — assert on
  `$.content` / `$.content[0]`, not `$[0]`, and seed synthetic identifiers
  (e.g. `IT-PART-3001`, synthetic set numbers) to avoid collisions.
- Use explicit pagination params in assertions (`page=0`, `size=10`, `sort=id,asc`).
- Keep integration tests focused on real flows; push edge-case permutations down to
  unit tests.

## Known Gaps (TODO)

- No CI runner executes these yet; they must be run locally. See
  [testing-strategy.md](./testing-strategy.md#whats-missing-todo).
