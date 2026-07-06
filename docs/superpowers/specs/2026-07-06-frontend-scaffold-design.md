# Frontend Scaffold + Set Search — Design

Date: 2026-07-06
Status: Approved

## Goal

Scaffold the BrickDeck frontend (`apps/web`) and deliver the Phase 1 success-criterion UI: a user can search a LEGO set. Set detail + parts inventory pages follow in later slices.

## Stack

Next.js (App Router) · React · TypeScript (strict) · **MUI (Material UI) + Emotion** · TanStack Query · React Hook Form + Zod · Vitest + React Testing Library. Package manager: **npm** (pnpm not installed).

MUI replaces the originally-planned Tailwind + shadcn/ui (user decision 2026-07-06 — consistency with sibling `brewdeck` project). No Tailwind, no shadcn.

## Architecture

```
apps/web/
  app/
    layout.tsx            root: AppRouterCacheProvider + ThemeProvider + QueryProvider
    page.tsx              home → link to /sets
    sets/page.tsx         set search page
  src/
    lib/
      env.ts              API base URL from NEXT_PUBLIC_API_BASE_URL
      api/
        client.ts         typed fetch wrapper (JSON, error normalization)
        sets.ts           searchSets(query, page, size) → PageResponse<BrickSetResponse>
      types/
        api.ts            PageResponse<T>, BrickSetResponse (mirror backend DTOs)
      query/
        keys.ts           centralized query keys
    providers/
      QueryProvider.tsx   'use client' QueryClientProvider
      ThemeProvider.tsx   'use client' MUI theme
    features/sets/
      useSetSearch.ts     TanStack useQuery hook
      SetSearchBar.tsx    RHF + Zod search input
      SetResults.tsx      results grid + loading/error/empty states
      SetCard.tsx         single set card
```

### Data flow

`SetSearchBar` (RHF+Zod) → submits query → `sets/page.tsx` holds `{query, page}` in URL/local state → `useSetSearch` (`useQuery`, keyed on query+page, `enabled` only when query non-empty) → `searchSets` → `apiClient` → `GET /api/v1/sets/search?q=&page=&size=` → `PageResponse<BrickSetResponse>` → `SetResults` renders cards + simple prev/next pagination.

### API client

- Single `apiClient` wrapper around `fetch`: sets `Accept: application/json`, base URL from env, throws a normalized `ApiError` on non-2xx (status + parsed body message). No component calls `fetch` directly.
- Base URL: `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`). Backend CORS already allows `http://localhost:3000`.

### Types

Hand-written TS interfaces mirroring backend DTOs for this slice (`PageResponse<T>`, `BrickSetResponse` with `externalSetNumber`, `name`, `year`, `externalThemeId`, `externalUrl`, `cacheStatus`). Future: generate from the OpenAPI spec (`/v3/api-docs`) — out of scope here.

## Error / loading / empty states

`SetResults` renders: MUI `CircularProgress` while loading, an error `Alert` on `ApiError` (message from backend), an empty-state message when the query returns zero content, and the card grid otherwise. Idle (no query yet) shows a prompt to search.

## Testing (Vitest + RTL, TDD)

- `apiClient` / `searchSets`: mock `fetch`, assert URL + params + error normalization.
- `useSetSearch`: wrapped in `QueryClientProvider`, mock the api layer, assert enabled-gating and data mapping.
- `SetSearchBar`: Zod validation (empty query blocked), submit calls handler.
- `SetResults`: loading, error, empty, and populated states render correctly.
- Search page integration: type query → submit → results render (api layer mocked).

TDD: each unit gets a failing test first, then minimal implementation.

## Out of scope (later slices)

Set detail page, parts inventory table, theme browsing, infinite scroll, OpenAPI type generation, auth. Pagination here is minimal prev/next.

## Verification

```bash
cd apps/web
npm run lint
npm run test
npm run build
```
Manual: backend on `:8080`, `npm run dev`, search a real set at `/sets`.
```
