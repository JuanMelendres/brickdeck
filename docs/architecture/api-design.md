# API Design

REST/JSON API served by `apps/api` (Spring Boot). Base URL in local dev:
`http://localhost:8080`. Machine-readable contract: `/v3/api-docs`
(also mirrored in [`docs/api/openapi.yaml`](../api/openapi.yaml)). Interactive:
`/swagger-ui/index.html`.

For request/response conventions, error codes, and auth details also see
[`docs/api/README.md`](../api/README.md).

## Conventions

- **Versioning:** endpoints are under `/api/v1/*`.
  (Two legacy paths remain un-versioned — see the Inconsistencies note below.)
- **DTOs only:** controllers return DTO records, never entities or raw Rebrickable payloads.
- **Detail endpoints** return the DTO directly; **list endpoints** return `PageResponse<T>`.
- **Pagination** is 0-indexed with `page`, `size`, `sort` (`@PageableDefault`).
- **Auth:** collection endpoints require `Authorization: Bearer <jwt>`. Catalog and auth endpoints are public.

## `PageResponse<T>` envelope

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Endpoints

### Health
| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | public | Liveness. Static `UP` — deliberately checks no dependencies. |
| GET | `/actuator/health` | public | Readiness. Checks the datasource: `200 {"status":"UP"}` / `503 {"status":"DOWN"}`. |

The split is intentional. **Liveness** answers "is the process alive?" and must not check
dependencies — restarting the pod cannot fix a database outage, so a liveness probe that
checks the database turns one outage into a restart loop. **Readiness** answers "can I
serve traffic?" and must check them, so a pod with no database is pulled from the load
balancer without being killed.

Both are public by necessity: authenticating a health endpoint loads a user from the
database, so a database outage would answer `401` instead of `DOWN` — failing exactly when
a probe needs the truth. Only `/actuator/health` is exposed; the rest of actuator stays
authenticated, and `show-details` keeps its `never` default, so the body is just
`{"status":"UP"}` with no component or dependency detail.

### Auth
| Method | Path | Auth | Body | Result |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | public | `RegisterRequest` | `201` `AuthResponse` |
| POST | `/api/v1/auth/login` | public | `LoginRequest` | `200` `AuthResponse` |
| GET | `/api/v1/auth/me` | Bearer | — | `UserResponse` |

### Catalog — Sets
| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/v1/sets` | public | Paginated `PageResponse<BrickSetResponse>` (`size=20`, `sort=externalSetNumber`). |
| GET | `/api/v1/sets/by-number/{setNumber}` | public | Find-or-import; returns `BrickSetResponse`. |
| GET | `/api/v1/sets/search?q=&page=&size=` | public | Rebrickable fuzzy search, `PageResponse`. |
| GET | `/api/v1/sets/{setNumber}/parts` | public | Inventory, `PageResponse<SetPartResponse>` (`size=50`). |
| POST | `/api/v1/catalog/sets/import` | public | Import a set (`ImportSetRequest`). |
| GET | `/api/v1/catalog/sets/external/{setNumber}` | public | Raw external lookup (debug/inspection). |
| POST | `/api/v1/catalog/sets/{setNumber}/inventory/import` | public | Import inventory; `InventoryImportResult`. |

### Catalog — Themes
| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/catalog/themes/{id}` | public | `ThemeResponse`. *(un-versioned — see below)* |

### Collection — Sets (Bearer required)
| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/api/v1/collection/sets` | `AddUserSetRequest` | `201` + `Location`, `UserSetResponse` |
| GET | `/api/v1/collection/sets` | — | `PageResponse<UserSetResponse>` (`size=20`, `sort=createdAt DESC`) |
| PATCH | `/api/v1/collection/sets/{id}` | `UpdateUserSetRequest` | `UserSetResponse` |
| DELETE | `/api/v1/collection/sets/{id}` | — | `204` |

### Collection — Loose Parts (Bearer required)
| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/api/v1/collection/parts` | `AddUserPartRequest` | `201` + `Location`, `UserPartResponse` |
| GET | `/api/v1/collection/parts` | — | `PageResponse<UserPartResponse>` (`size=20`, `sort=createdAt DESC`) |
| PATCH | `/api/v1/collection/parts/{id}` | `UpdateUserPartRequest` | `UserPartResponse` |
| DELETE | `/api/v1/collection/parts/{id}` | — | `204` |

### Missing Pieces (Bearer required)
| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/v1/sets/{setNumber}/missing-parts` | Bearer | `MissingPartsReport` — target set's required (non-spare) parts vs the user's owned inventory (loose parts + owned/built/in-progress sets); per part+color `required`/`owned`/`missing` plus `completionPercentage`. Query: `missingOnly` (default false), `page` (0), `size` (50) — totals are whole-set; `lines` are filtered+paginated. `404` if set or inventory not imported. |

## Error Handling

All errors flow through `GlobalExceptionHandler` (`@RestControllerAdvice`).

| Status | When |
| --- | --- |
| `400` | Bean Validation failure — body includes `validationErrors` map. |
| `401` | Invalid credentials / missing or invalid JWT. |
| `404` | `ResourceNotFoundException` (missing or not-owned resource). |
| `409` | Duplicate collection entry (`DuplicateCollectionEntryException`). |

Validation error shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/collection/sets",
  "validationErrors": { "setNumber": "must not be blank" }
}
```

## Known Inconsistencies (TODO)

- `GET /api/catalog/themes/{id}` is **not** under `/api/v1`. Other catalog paths are.
  Consider versioning it (or documenting the exception) before the API is public.
- `GET /api/v1/catalog/sets/external/{setNumber}` returns a Rebrickable-shaped
  response (`RebrickableSetResponse`). This is an inspection/debug endpoint; it
  intentionally exposes the external shape and should not be consumed by the frontend.
