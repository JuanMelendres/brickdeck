# Local Setup

## Requirements

- **JDK 21** (backend).
- **Node.js 20+** and npm (frontend).
- **Docker** + Docker Compose (local PostgreSQL).
- A **Rebrickable API key** for catalog import ([get one here](https://rebrickable.com/api/)).

## 1. Environment

Copy the template and fill in values (at minimum `REBRICKABLE_API_KEY` and a
strong `JWT_SECRET`):

```bash
cp .env.example .env
cp apps/web/.env.example apps/web/.env
```

See [environment-variables.md](./environment-variables.md) for every variable.

## 2. Database

```bash
docker compose up -d          # PostgreSQL 16 on localhost:5433
nc -z -w2 localhost 5433 && echo "db up"
```

Flyway applies migrations automatically when the API starts.

## 3. Backend (`apps/api`)

```bash
cd apps/api
./mvnw spring-boot:run
```

- API: <http://localhost:8080>
- Health: <http://localhost:8080/api/v1/health>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>

## 4. Frontend (`apps/web`)

```bash
cd apps/web
npm install
npm run dev
```

- Web: <http://localhost:3000> (CORS is pre-allowed for this origin).
- Optionally refresh generated API types (API must be running):
  `npm run gen:api`.

## 5. Run the tests

```bash
# backend (needs PostgreSQL on 5433 for the full suite)
cd apps/api && ./mvnw clean verify

# frontend
cd apps/web && npm run test && npm run typecheck && npm run lint
```

See [../testing/unit-testing.md](../testing/unit-testing.md) and
[../testing/integration-testing.md](../testing/integration-testing.md).

## Troubleshooting

- **Port 5433 in use / DB refused:** ensure Docker is running and no other
  PostgreSQL owns the port; `docker compose logs brickdeck-postgres`.
- **Flyway validation error:** the schema drifted from migrations — reset the dev DB
  with `docker compose down -v && docker compose up -d`.
- **Rebrickable 401/empty imports:** `REBRICKABLE_API_KEY` is missing or invalid.
- **Noisy Maven output:** Mockito self-attach / CDS warnings are benign; filter with
  `2>&1 | grep -E "Tests run:|BUILD"`.
