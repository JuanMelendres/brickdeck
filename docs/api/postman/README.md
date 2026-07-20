# Postman Collection

- [`brickdeck.postman_collection.json`](./brickdeck.postman_collection.json) — the API collection.
- [`brickdeck.local.postman_environment.json`](./brickdeck.local.postman_environment.json) — local environment (`baseUrl = http://localhost:8080`).

## Usage

1. Import both files into Postman.
2. Select the **BrickDeck Local** environment.
3. Run **Auth > Login** first — its test script stores the JWT in `{{token}}`.
   The collection sends `{{token}}` as a Bearer token by default; the public
   Catalog requests override this with no-auth.
4. Add-to-collection requests store the created `id` into `{{userSetId}}` /
   `{{userPartId}}` for the follow-up update/delete requests.

## Layout

- `Auth` — register, login (saves token), me
- `Catalog` — list, search, by-number, import, inventory import, parts,
  missing-parts, **compare** (`GET /api/v1/sets/compare?a=&b=&category=`)
- `Recommendations` — buildable wishlist sets (`GET /api/v1/recommendations/buildable`)
- `Pricing` — add/list/delete price snapshots, price analysis + deal verdict, alert rules (create/list/delete), triggered alerts
- `Collection - Sets` — add, list, update, delete
- `Collection - Parts` — add, list, update, delete

The `category` query param on **Compare** is included but disabled by default;
enable it to filter lines by `ONLY_A`, `ONLY_B`, or `BOTH`.

## Keeping it current

Regenerate from the live spec (`/v3/api-docs`) or [`../openapi.yaml`](../openapi.yaml)
when endpoints change, or edit this collection directly. No real secrets or
Rebrickable API keys belong in these files — credentials live in the
environment as placeholders.
