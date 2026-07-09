# Test Plan

A snapshot of what is covered by automated tests today and where the gaps are.
Update alongside feature work.

## Backend Coverage

| Area | Test classes | Notes |
| --- | --- | --- |
| Auth | `AuthServiceTest`, `JwtServiceTest`, `AuthIntegrationTest` | register/login/me, hashing, JWT. |
| Catalog — sets | `BrickSetServiceTest`, `BrickSetControllerTest`, `BrickSetImportControllerTest` | find-or-import both ways, import, list/search. |
| Catalog — inventory | `SetInventoryServiceTest`, `SetInventoryImportControllerTest`, `SetPartControllerTest`, `SetPartRepositoryTest`, `ColorServiceTest`, `PartServiceTest` | idempotent import, reads, reference upserts. |
| Catalog — themes | `ThemeServiceTest`, `ThemeControllerTest` | resolution/upsert, read. |
| External client | `RebrickableClientTest` | mapping, timeouts. |
| Collection — sets | `CollectionServiceTest`, `CollectionControllerTest`, `CollectionIntegrationTest` | add/list/update/delete, dup 409, owner 404. |
| Collection — parts | `UserPartServiceTest`, `UserPartControllerTest`, `UserPartIntegrationTest` | quantity by part+color, dup 409, missing ref 404. |
| Config | `CorsConfigTest`, `OpenApiDocsTest` | CORS preflight, OpenAPI docs endpoint. |

## Frontend Coverage

| Area | Covered |
| --- | --- |
| API client | request shaping, error handling. |
| Set search | hook + `SetSearchBar` / `SetResults` / `SetCard`, loading/error/empty/pagination. |
| Set detail | `SetDetail` / `PartsInventory` hooks + components, import action. |

## Gaps / TODO

- **CI:** none. Tests are local-only. Highest-priority gap.
- **Frontend auth + collection UI:** not built yet, therefore untested.
- **Coverage reporting:** not configured (SonarCloud planned).
- **Contract test:** no automated check that `docs/api/openapi.yaml` matches
  `/v3/api-docs` — consider a lightweight diff check later.

## Regression Checklist (manual, pre-merge)

```bash
# backend
cd apps/api && nc -z -w2 localhost 5433 && ./mvnw clean verify 2>&1 | grep -E "Tests run:|BUILD"
# frontend
cd apps/web && npm run test && npm run typecheck && npm run lint && npm run build
```
