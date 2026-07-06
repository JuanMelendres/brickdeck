---
name: frontend-nextjs
description: Use this skill when starting or modifying the BrickDeck frontend using Next.js, React, TypeScript, API clients, forms, pagination UI, filters, catalog/collection pages, or frontend architecture.
---

# Frontend Next.js Skill

## Role

Act as a senior frontend engineer using Next.js (App Router), React, TypeScript, Tailwind, and shadcn/ui.

## Tech Stack & Assumptions

- **Framework:** Next.js (App Router) in `apps/web` (not yet scaffolded)
- **Language:** TypeScript (strict; avoid `any`)
- **Styling:** Tailwind CSS + shadcn/ui
- **Server state:** TanStack Query (do not store server data in global client state)
- **Client state:** Zustand or Redux Toolkit for shared UI state; Context only for DI (theme/auth)
- **Forms:** React Hook Form + Zod
- **Testing:** Vitest + React Testing Library

## Planned Screens (Phase 1 catalog first)

- Catalog search
- Set detail (metadata + parts inventory)
- Theme browse
- (Later) collection, missing pieces, comparison, recommendations

## API Assumptions

- Consumes the Spring Boot API at `http://localhost:8080`.
- Detail endpoints return DTOs (e.g. `BrickSetResponse` with `externalThemeId`, `externalUrl`, `cacheStatus`).
- Collection endpoints return `PageResponse<T>`:

```ts
type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
```

## Frontend Rules

- TypeScript types for all API responses.
- An API client layer — do not call fetch directly from every component.
- TanStack Query for data fetching/caching; typed, validated forms.
- Support pagination and filters on list pages.
- Small, reusable, single-responsibility components; extract logic into hooks.
- Memoize (`React.memo`/`useMemo`/`useCallback`) in heavy data tables.
- Lazy-load (`React.lazy`) distinct routes/heavy modules.
- Handle loading, empty, and error states (error boundaries).

## Testing Rules

- Unit-test complex components, custom hooks, utilities.
- Test behavior/accessibility with RTL, not implementation details.
- Mock the API client layer in page tests.

## Before Starting Frontend

Confirm the backend has:

- CORS for `http://localhost:3000`
- Stable catalog DTOs
- `PageResponse` on list endpoints
- Swagger/OpenAPI available
