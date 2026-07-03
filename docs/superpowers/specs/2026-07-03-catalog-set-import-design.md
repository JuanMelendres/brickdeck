# Catalog Set Import — Design

Date: 2026-07-03
Status: Approved (pending spec review)
Area: `apps/api` — `com.brickdeck.api.catalog`, `com.brickdeck.api.external.rebrickable`

## Goal

Add an explicit write endpoint to import a LEGO set from Rebrickable into the
local catalog, replacing the current implicit "import as a GET side effect"
behavior. Importing a set also fetches and links its Theme so the existing
(currently unused) `BrickSet.theme` foreign key becomes meaningful.

## Decisions (locked)

| Decision | Choice |
|----------|--------|
| Write shape | Single-set import via `POST` only. No bulk, no refresh endpoint. |
| Endpoint path | `POST /api/v1/catalog/sets/import` |
| Existing-set behavior | Idempotent upsert: re-pull from Rebrickable and update the row. |
| HTTP status | `201 Created` when a new row is created; `200 OK` when an existing row is refreshed. |
| Theme handling | Fetch `/lego/themes/{id}`, upsert the Theme row, link `BrickSet.theme` FK. |
| Theme parent | `parent_theme_id` left `null` — theme hierarchy is a later feature. |
| GET by-number | Becomes read-only: return the local row or `404`. No write-on-read. |

## API Surface

### `POST /api/v1/catalog/sets/import`

Lives under the `/api/v1/catalog/sets` base path (alongside the raw Rebrickable
proxy `GET /external/{setNumber}`), in a new focused controller so the raw
proxy controller stays single-purpose.

Request body:

```json
{ "setNumber": "75375-1" }
```

- `setNumber` is `@NotBlank`. A blank/missing value yields `400` via Spring's
  bean-validation handling (`spring-boot-starter-validation` is already a
  dependency).

Responses:

- `201 Created` — set did not exist locally; imported now.
- `200 OK` — set already existed; row refreshed from Rebrickable.
- `404 Not Found` — Rebrickable has no such set (`{ "message": "..." }`).
- `400 Bad Request` — blank `setNumber`.

Success body is the existing `BrickSetResponse`, with two new `cacheStatus`
values:

- `IMPORTED_FROM_REBRICKABLE` — returned on `201`.
- `REFRESHED_FROM_REBRICKABLE` — returned on `200`.

(The existing `LOCAL_CACHE_HIT` value is unchanged and continues to be used by
the read endpoints.)

### `GET /api/v1/sets/by-number/{setNumber}` (changed)

Refactor `BrickSetService.findOrImportBySetNumber` → `findBySetNumber`:

- Returns the local `BrickSetResponse` (`cacheStatus = LOCAL_CACHE_HIT`) when
  present.
- Throws `ResourceNotFoundException` → `404` when absent. No Rebrickable call,
  no write.

This enforces command/query separation: reads never mutate.

## Import Flow

`BrickSetService.importSet(String setNumber)`:

```
external = rebrickableClient.getSetByNumber(setNumber)
    // Rebrickable 404 -> ResourceNotFoundException("Set not found in Rebrickable: {setNumber}")

theme = themeService.resolveByExternalId(external.themeId())
    // may be null when external.themeId() is null

brickSet = brickSetRepository.findByExternalSetNumber(external.setNum())
                             .orElseGet(BrickSet::new)
created = (brickSet.getId() == null)

// map all catalog fields from `external`
brickSet.setExternalSetNumber(external.setNum())
brickSet.setName(external.name())
brickSet.setYearReleased(external.year())
brickSet.setExternalThemeId(external.themeId())
brickSet.setNumberOfParts(external.numParts())
brickSet.setImageUrl(external.setImgUrl())
brickSet.setExternalUrl(external.setUrl())
brickSet.setExternalLastModifiedAt(parse(external.lastModifiedDt()))
brickSet.setTheme(theme)
brickSet.setSource("REBRICKABLE")

saved = brickSetRepository.save(brickSet)
return new ImportResult(created, toResponse(saved, created ? IMPORTED : REFRESHED))
```

`ImportResult(boolean created, BrickSetResponse body)` is a small internal
record so the controller can pick `201` vs `200` without re-deriving state.

`ThemeService.resolveByExternalId(Integer externalThemeId)`:

```
if externalThemeId == null: return null

external = rebrickableClient.getThemeById(externalThemeId)
theme = themeRepository.findByExternalId(String.valueOf(externalThemeId))
                      .orElseGet(Theme::new)
theme.setExternalId(String.valueOf(externalThemeId))
theme.setName(external.name())
// parent_theme_id intentionally left null (hierarchy is a later feature)
return themeRepository.save(theme)
```

Both services run in `@Transactional` (write). `resolveByExternalId` is called
within the set-import transaction so the Theme and BrickSet commit together.

### Rebrickable error mapping

`RestClient` throws `HttpClientErrorException.NotFound` on a `404`. Map it to
`ResourceNotFoundException` so the existing `GlobalExceptionHandler` returns a
clean `404` body. Do the mapping in the service (catch around the
`getSetByNumber` call) rather than leaking `RestClient` exception types into the
web layer. `getThemeById` 404 is not expected (Rebrickable returns a theme id
that resolves); if it occurs it surfaces the same way — acceptable for the MVP.

## New / Changed Components

**Migration**
- `V3__add_unique_constraint_to_themes_external_id.sql`: add a unique constraint
  on `themes.external_id`. Guarantees idempotent theme upsert (one row per
  external theme). Postgres permits multiple `NULL`s under a unique constraint,
  so pre-existing null-external_id rows are unaffected.

**DTOs**
- `ImportSetRequest` — record `{ @NotBlank String setNumber }`.
- `RebrickableThemeResponse` — record `{ Integer id, String name, @JsonProperty("parent_id") Integer parentId }`.
- `ImportResult` — internal record `{ boolean created, BrickSetResponse body }`.

**Client**
- `RebrickableClient.getThemeById(Integer id)` → `GET /lego/themes/{id}/`,
  returns `RebrickableThemeResponse`.

**Repository**
- `ThemeRepository.findByExternalId(String externalId)` → `Optional<Theme>`.

**Entity**
- `Theme`: add `@PrePersist` (set `id` if null, `createdAt`, `updatedAt`) and
  `@PreUpdate` (set `updatedAt`), mirroring `BrickSet`. Currently `Theme` has no
  lifecycle callbacks, so upserts would fail to populate the non-null id/time
  columns.

**Service**
- `BrickSetService`: add `importSet`; refactor `findOrImportBySetNumber` →
  `findBySetNumber` (read-only, 404 on miss); inject `ThemeService`.
- `ThemeService`: add `resolveByExternalId`; inject `RebrickableClient` and use
  `ThemeRepository`.

**Controller**
- New `BrickSetImportController` at `/api/v1/catalog/sets`, `POST /import`,
  returns `ResponseEntity<BrickSetResponse>` with status from `ImportResult`.
- `BrickSetController.findOrImportBySetNumber` → `findBySetNumber` (delegates to
  the renamed service method).

## Testing

**`BrickSetServiceTest`** (unit, Mockito)
- `importSet` creates a new row when absent → `created=true`, cacheStatus
  `IMPORTED_FROM_REBRICKABLE`, theme fetched and linked.
- `importSet` refreshes an existing row → `created=false`, cacheStatus
  `REFRESHED_FROM_REBRICKABLE`, fields overwritten from Rebrickable.
- `importSet` with a set whose `themeId` is null → theme not fetched, FK null.
- `importSet` maps Rebrickable `404` → `ResourceNotFoundException`.
- Rework the current `findOrImportBySetNumber` tests to `findBySetNumber`:
  returns local row when present; throws `ResourceNotFoundException` when absent
  (and does **not** call Rebrickable).

**`ThemeServiceTest`** (unit, Mockito)
- `resolveByExternalId` creates a new Theme when none exists.
- `resolveByExternalId` reuses the existing Theme (found by external id) and
  updates its name.
- `resolveByExternalId(null)` returns null without calling Rebrickable.

**`BrickSetImportControllerTest`** (MockMvc / `@WebMvcTest`)
- `201` on new import (body + `Location`-free, cacheStatus asserted).
- `200` on refresh.
- `400` on blank `setNumber`.
- `404` when the service throws `ResourceNotFoundException`.

## Out of Scope

- Bulk import / search-and-import.
- A dedicated refresh endpoint (upsert on `POST` covers re-pull).
- Theme parent hierarchy (`parent_theme_id`).
- Set parts/inventory import (later roadmap item).
- Resilience4j circuit breakers / retries (add when external call volume grows).
