# BrickDeck Environment Setup

This document defines the environment strategy for BrickDeck.

---

## Environment Files

Use environment files locally, but never commit real secrets.

Recommended files:

```text
.env.example        # committed, placeholders only
.env.local          # local frontend secrets, not committed
.env                # local root environment, not committed
.env.docker         # local Docker environment, not committed if it contains secrets
```

---

## Required Variables

Initial placeholders:

```env
# Database
POSTGRES_DB=brickdeck
POSTGRES_USER=brickdeck
POSTGRES_PASSWORD=brickdeck_dev_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# Backend
SPRING_PROFILES_ACTIVE=local
API_PORT=8080

# Frontend
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# External APIs
REBRICKABLE_API_KEY=your_rebrickable_api_key_here
BRICKLINK_CONSUMER_KEY=your_bricklink_consumer_key_here
BRICKLINK_CONSUMER_SECRET=your_bricklink_consumer_secret_here
BRICKSET_API_KEY=your_brickset_api_key_here
```

---

## Secret Handling Rules

- Never commit real API keys.
- Never paste secrets into documentation.
- Never upload `.env` files to GitHub.
- Use `.env.example` for placeholders only.
- Use GitHub Actions secrets for CI/CD.
- Rotate keys if accidentally exposed.

---

## Local Development Goal

The project should eventually run locally with:

```bash
docker compose up -d
```

Then:

```bash
# Backend
cd services/api
./mvnw spring-boot:run

# Frontend
cd apps/web
npm run dev
```

Exact commands may change depending on final tooling.
