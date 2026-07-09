# Environment Variables

Configuration is provided via environment variables / `.env` files. **Never commit
real secrets.** Commit `.env.example` (placeholders only). The template is at the
repository root: [`.env.example`](../../.env.example).

## How they are loaded

`apps/api` (`application.yaml`) imports, if present, in order:
`.env`, `.env.local`, `../../.env`, `../../.env.local`. So a single root `.env`
works for the backend. `apps/web` reads `NEXT_PUBLIC_*` vars from its own
`apps/web/.env` (see [`apps/web/.env.example`](../../apps/web/.env.example)).

## Backend / root variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_DB` | `brickdeck` | Database name. |
| `POSTGRES_USER` | `brickdeck` | Database user. |
| `POSTGRES_PASSWORD` | `brickdeck` | Database password (dev default). |
| `POSTGRES_HOST` | `localhost` | Database host. |
| `POSTGRES_PORT` | `5433` | Host port mapped by Docker Compose. |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile. |
| `REBRICKABLE_BASE_URL` | `https://rebrickable.com/api/v3` | Rebrickable API base URL. |
| `REBRICKABLE_API_KEY` | _(empty)_ | Rebrickable API key — **required** for catalog import. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed browser origins for `/api/**`. |
| `JWT_SECRET` | dev-only fallback | HS256 signing secret — **must be >= 32 bytes**; override everywhere. |
| `JWT_EXPIRATION_MINUTES` | `120` | JWT lifetime in minutes. |

## Frontend variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Base URL of the backend API (exposed to the browser). |

> Only `NEXT_PUBLIC_*` variables are exposed to the browser. Never put secrets in a
> `NEXT_PUBLIC_*` variable.

## Secret-handling rules

- Never commit real API keys or `.env` / `.env.local` files.
- Never paste secrets into documentation.
- Use `.env.example` for placeholders only.
- In CI/CD, use the platform's secret store (e.g. GitHub Actions secrets).
- Rotate any key that is accidentally exposed.

> **Note:** Older docs referenced `BRICKLINK_*` and `BRICKSET_*` keys. Those
> integrations are not implemented; only `REBRICKABLE_API_KEY` is used today.
