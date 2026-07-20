# Features

Status legend: **Done** (shipped) · **In progress** · **Planned**.

## Catalog

| Feature | Status | Notes |
| --- | --- | --- |
| Search LEGO sets | Done | Rebrickable fuzzy search, paginated. Backend + web UI. |
| View set details | Done | Backend + web UI (`/sets/[setNumber]`). |
| View set parts inventory | Done | Import-on-demand, paginated. Backend + web UI. |
| Theme resolution / upsert | Done | Themes normalized and cached locally. |
| Local caching of external data | Done | Find-or-import is cache-first. |

See FDDs: [set-search](./fdd/set-search-fdd.md), [set-inventory](./fdd/set-inventory-fdd.md).

## User Collection

| Feature | Status | Notes |
| --- | --- | --- |
| Authentication (register/login) | Done | Stateless JWT. Backend only. |
| Add set to collection | Done | Status, purchase price, purchase date. Backend only. |
| Update / remove collection set | Done | Partial update (PATCH), delete. Backend only. |
| Loose piece inventory | Done | Quantity by part + color, storage location. Backend only. |
| Frontend auth wiring | In progress | Login/register UI, token storage, protected routes. |
| Frontend collection UI | Planned | Sets + loose parts management screens. |

See FDDs: [authentication](./fdd/authentication-fdd.md),
[collection-sets](./fdd/collection-sets-fdd.md),
[collection-loose-parts](./fdd/collection-loose-parts-fdd.md).

## Planned Engines (not started)

| Feature | Status | Notes |
| --- | --- | --- |
| Missing-pieces engine | Done | `GET /sets/{setNumber}/missing-parts` (required vs owned = loose + owned sets, completion %, `missingOnly` filter + line pagination) and a "Missing pieces" panel on the set-detail page (completion bar, only-missing toggle, paging). See [FDD](./fdd/missing-pieces-fdd.md). |
| Set comparison engine | Planned | Version diff, shared/unique parts, similarity score. |
| Build recommendation engine | Planned | Buildable / almost-buildable sets from inventory. |
| Price tracking & deals | Planned | API/feed-based; no aggressive scraping. |
| AI piece recognition | Planned | Photo to part/color suggestion + confidence. |

## Collection Status Values (implemented)

`OWNED`, `WISHLIST`, `BUILT`, `IN_PROGRESS` (see `CollectionStatus`).

> **Assumption:** The README lists richer statuses (For sale, Sold, Archived,
> Missing pieces). Only the four above are implemented today. Extra statuses are
> future scope.
