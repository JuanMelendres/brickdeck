# Coding Standards

## Backend (Java / Spring Boot)

- **DDD + hexagonal:** organize by domain (`catalog`, `collection`, `security`);
  keep external clients isolated in `external.*`. See [ADR-009](../decisions/ADR-009-hexagonal-ddd-packages.md).
- **Never expose entities.** Controllers return DTO records (Java records) only.
- **Never expose raw external payloads.** Normalize Rebrickable responses into
  internal entities first.
- **Validation** with Bean Validation on request records. Throw
  `ResourceNotFoundException` (in `common`) for missing resources. Avoid special
  symbols in messages (write `degrees Celsius`, not the symbol).
- **Timestamps:** set `@PrePersist` / `@CreationTimestamp` before persisting entities
  with non-null `created_at` / `updated_at`.
- **Persistence:** avoid N+1 (use entity graphs / projections). Migrations are the
  source of truth (`ddl-auto: validate`); never rely on Hibernate to change schema.
- **Caching:** find-or-import is cache-first — a local hit skips Rebrickable and skips save.

## Frontend (React / TypeScript)

- **Strict TypeScript.** No `any`; use precise interfaces or `unknown` + guards.
  Type all API responses.
- **Functional components + hooks only.** Small, single-responsibility components;
  extract logic into custom hooks.
- **API client layer:** never call `fetch` directly from components — go through the
  `lib/api` client.
- **Server state:** TanStack Query for all API data; never store server data in global
  client state. Centralize query keys; invalidate the right prefixes in mutation `onSuccess`.
- **Client state:** `useState`/`useReducer` for local; Zustand/Redux Toolkit only for
  shared UI state; React Context only for DI (theme/auth).
- **Forms:** React Hook Form + `zodResolver`; map server `400` validation errors back onto fields.
- **UI/UX:** always handle loading, error, and empty states.
- **Styling:** MUI `sx` / `styled` + shared `ThemeProvider` (no Tailwind/shadcn — [ADR-007](../decisions/ADR-007-mui-frontend.md)).

## Testing

TDD: failing test (RED) to minimal pass (GREEN) to refactor. Every meaningful change
ships with tests. See [../testing/testing-strategy.md](../testing/testing-strategy.md).

## Anti-Patterns (do not)

1. Return JPA entities from controllers.
2. Expose raw Rebrickable responses.
3. Re-fetch on a cache hit.
4. Use `@MockBean` (use `@MockitoBean`).
5. Persist entities without timestamps set.
6. Use `any` in TypeScript.
7. Store server data in global client state.
8. Assert `$[0]` on paginated bodies (use `$.content[0]`).
9. Put special symbols in validation messages.
10. Hardcode secrets (use env vars + `.env.example`).
