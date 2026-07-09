# Postman Collection

**TODO:** No Postman collection has been exported yet. When one is created, place
it here as `brickdeck.postman_collection.json` and link it from
[`../README.md`](../README.md).

## How to generate

Two options:

1. **Import the OpenAPI spec into Postman.** Import
   [`../openapi.yaml`](../openapi.yaml) (or the live `/v3/api-docs`) — Postman
   generates a collection from it.
2. **Use the project's update-postman command** (`.claude/commands/update-postman.md`)
   to build/refresh the collection from the current endpoints.

## Suggested collection layout

- `Auth` — register, login, me
- `Catalog` — search, by-number, list, inventory import, parts
- `Collection - Sets` — add, list, update, delete
- `Collection - Parts` — add, list, update, delete

Store the JWT in a `{{token}}` collection variable and set the `Authorization`
header at the collection level for the `Collection` folders.
