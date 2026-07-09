# ADR-007: MUI + Emotion for the Frontend (supersedes Tailwind/shadcn)

## Status
Accepted

## Date
2026-07-06

## Context
Early planning docs (the original README) suggested Tailwind CSS + shadcn/ui for
the frontend. When the Next.js app was actually scaffolded, a component-library
approach with a shared theme and built-in data components (tables, forms) was
preferred for the data-heavy catalog/collection UI.

## Decision
Use **MUI (Material UI) + Emotion** as the component library and styling solution
for `apps/web`. Do not use Tailwind or shadcn/ui. Server state uses TanStack Query;
forms use React Hook Form + Zod. Next.js App Router SSR for Emotion uses
`@mui/material-nextjs` `AppRouterCacheProvider` in the root layout, with a client
`ThemeProvider`.

## Consequences
- Positive: ready-made accessible components (tables, dialogs, inputs) for dense UIs.
- Positive: centralized theming via `ThemeProvider` and `sx`/`styled`.
- Negative: larger bundle than utility CSS; MUI-specific conventions to learn.
- Note: this **supersedes** the Tailwind/shadcn suggestion in older docs. Those
  references have been corrected.

## Alternatives Considered
- Tailwind + shadcn/ui: rejected in favor of a batteries-included component library.

## Notes
See [architecture/overview.md — Frontend Architecture](../architecture/overview.md#frontend-architecture)
and `apps/web`.
