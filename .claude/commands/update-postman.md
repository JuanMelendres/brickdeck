# Update Postman Collection

Review and update the BrickDeck Postman collection (create it under `docs/postman` if it does not exist yet).

## Location

```text
docs/postman/brickdeck.postman_collection.json
docs/postman/brickdeck.local.postman_environment.json
```

## Checklist

Check that:

- Endpoints match the current Spring Boot controllers under `catalog`:
  - `GET /api/v1/sets` (list / read)
  - `GET /api/v1/sets/by-number/{setNumber}` (find-or-import)
  - Set import endpoint
  - Theme endpoints
- Collection GET endpoints support `page`, `size`, `sort` once pagination is added.
- Variables use meaningful names (e.g. `{{setNumber}}`, `{{themeId}}`), not `{{$guid}}`.
- Request bodies match current request records (e.g. `ImportSetRequest`).
- Responses reflect `BrickSetResponse` fields (`externalThemeId`, `externalUrl`, `cacheStatus`).
- Validation examples are included where useful.
- Postman tests store created IDs/numbers into environment variables when possible.
- No real secrets, tokens, or Rebrickable API keys are committed.
- `baseURL` is managed through the Postman environment.
- The local environment file points to `http://localhost:8080`.

## Output

1. Summary of changes.
2. Any endpoint mismatches found.
3. Any missing examples.
4. Updated files if changes are needed.
5. Suggested Conventional Commit message.
