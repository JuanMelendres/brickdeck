# BrickDeck Roadmap (working summary)

Full roadmap: `docs/ROADMAP.md`. This is the condensed working view.

## Phase 0 — Foundation

Status: Completed

- Repository, README, docs (architecture, API, AI, scraping strategy)
- Project structure (`apps/api`, `apps/web`, `docs`, `infra`)
- Claude configuration
- Docker Compose + environment strategy

## Phase 1 — Catalog Foundation

Status: Completed

- Spring Boot API + PostgreSQL + Flyway — Done
- Rebrickable API client — Done
- Theme upsert + resolution — Done
- Set import (upsert) + read-only lookup — Done
- Catalog search endpoint (Rebrickable-backed, `PageResponse`) — Done
- Set inventory (parts, colors) import + read — Done (schema, client, import service, endpoints)
- Global CORS config for `/api/**` (pre-frontend) — Done
- OpenAPI spec + Swagger UI (springdoc, pre-frontend) — Done
- Scaffold Next.js frontend (`apps/web`) + set search page — Done (MUI, TanStack Query, RHF+Zod, Vitest)

Success criteria: user can search a LEGO set and view its parts inventory. Backend met; frontend search shipped, set-detail/parts-inventory UI deferred to next slice.

## Phase 2 — User Collection

Status: Not Started

- Auth, add set to collection, set status, purchase price/date
- Loose pieces manual inventory, quantity by part/color, storage location

## Phase 3 — Missing Pieces Engine

Status: Not Started

- Compare required parts vs user inventory; missing pieces; completion %

## Phase 4 — Set Comparison Engine

Status: Not Started

- Side-by-side compare, inventory overlap, similarity score, diff summary

## Phase 5 — Build Recommendation Engine

Status: Not Started

- Recommend buildable / almost-buildable sets from inventory

## Phase 6 — Price Tracking and Deals

Status: Not Started

- Price snapshots, history, discount detection, wishlist alerts (APIs/feeds, no aggressive scraping)

## Phase 7 — AI-Assisted Classification

Status: Not Started

- Photo → part/color suggestion + confidence + user confirm

## Phase 8 — Productization

Status: Not Started

- Subscriptions, limits, premium AI, notifications, deployment, legal
