# Set Comparison Engine — Design

**Date:** 2026-07-13
**Phase:** 4 — Set Comparison Engine
**Status:** Approved (backend slice)

## Goal

Let a user compare two catalog sets by their part inventories: see which
part+color lines are unique to each set, which are shared, and a single
similarity score summarizing how alike the two sets are. This is the first
(backend) slice of Phase 4; a frontend compare page follows in a later slice.

## Scope

- **In:** backend engine + one read endpoint comparing two catalog sets.
- **Out (this slice):** frontend UI, set-vs-user-collection comparison (already
  covered by the missing-pieces engine), configurable similarity metrics.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Operands | Two catalog sets (A vs B) | Catalog-only, no auth, pure/cacheable. |
| Similarity | Quantity-weighted overlap `Σ min / Σ max` | Rewards shared pieces *and* matching counts. |
| Diff lines | Paginated + optional category filter | Mirrors the missing-pieces idiom; bounds payload. |
| Spares | Excluded (`spare = false` only) | Consistent with missing-pieces "required" rule. |

## Architecture

New `com.brickdeck.api.comparison` package, mirroring `missingpieces` (DDD /
hexagonal): `controller`, `service`, `dto`. Catalog-only and **public** (no
authentication), like the other set read endpoints.

**No new repository.** Reuses the existing
`SetPartRepository.findByBrickSet_ExternalSetNumberAndSpareFalse(String)`
(already annotated `@EntityGraph` on `part` + `color`), which the missing-pieces
engine also uses. The service is a pure, deterministic transform over the two
fetched inventories — trivial to unit test and safe to cache later.

## Endpoint

```
GET /api/v1/sets/compare?a={setNumber}&b={setNumber}&category=&page=0&size=50
```

- `a`, `b` (required): set numbers. Normalized via `catalog.service.SetNumbers`
  (bare `42232` → default variant `42232-1`) before lookup.
- `category` (optional): filter lines to one of `ONLY_A | ONLY_B | BOTH`.
  Absent → all lines.
- `page` (default 0), `size` (default 50): 0-indexed line pagination.
- **404** (`ResourceNotFoundException`) if either set is not imported locally or
  has no imported inventory — same contract as `GET /{setNumber}/missing-parts`.
- Route note: the literal `compare` segment resolves ahead of the
  `/api/v1/sets/{setNumber}` path variable in Spring's matcher, so there is no
  collision with the set-by-number read endpoint.

## Algorithm (service, pure)

1. Fetch non-spare `SetPart`s for A and for B via the shared repository method.
2. Build two maps keyed `(partId, colorId) → summed quantity` (a set may list
   the same part+color across multiple non-spare rows; sum them defensively).
3. Union the key sets. For each key produce a line:
   - `quantityA`, `quantityB` (0 when absent on that side)
   - `shared = min(quantityA, quantityB)`
   - `category`: `BOTH` if both > 0, else `ONLY_A` or `ONLY_B`.
4. `similarityScore = Σ min(quantityA, quantityB) / Σ max(quantityA, quantityB)`
   over the union, rounded to 2 decimal places, range `0.0..1.0`.
   Defensive guard: if `Σ max == 0` (cannot occur given inventory is required)
   → `0.0`.
5. Whole-set summary counts (constant, independent of filter/paging):
   `sharedLineCount`, `onlyALineCount`, `onlyBLineCount`.
6. Sort lines by category (`BOTH` → `ONLY_A` → `ONLY_B`), then part number, then
   color external id. Apply the optional `category` filter, then paginate.
   `totalLines`/`totalPages`/`first`/`last` reflect the post-filter line set.

### Worked example

Set A = { (brick X, red): 4, (plate Y, blue): 2 };
Set B = { (brick X, red): 2, (tile Z, white): 6 }.

- Union keys: (X,red), (Y,blue), (Z,white).
- Lines: (X,red) qtyA 4 / qtyB 2 / shared 2 / BOTH; (Y,blue) 2 / 0 / 0 / ONLY_A;
  (Z,white) 0 / 6 / 0 / ONLY_B.
- `Σ min = 2 + 0 + 0 = 2`; `Σ max = 4 + 2 + 6 = 12`; similarity = `0.17`.
- Summary: sharedLineCount 1, onlyALineCount 1, onlyBLineCount 1.

## DTOs (records)

```java
public record SetComparisonReport(
        String setNumberA,
        String setNumberB,
        double similarityScore,
        int sharedLineCount,
        int onlyALineCount,
        int onlyBLineCount,
        List<SetComparisonLine> lines,
        int page,
        int size,
        long totalLines,
        int totalPages,
        boolean first,
        boolean last
) {}

public record SetComparisonLine(
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        int quantityA,
        int quantityB,
        int shared,
        ComparisonCategory category
) {}

public enum ComparisonCategory { ONLY_A, ONLY_B, BOTH }
```

The report folds summary + pagination into one bespoke record rather than
`PageResponse<T>`, matching the existing `MissingPartsReport` shape (a
`PageResponse` cannot carry the whole-set summary fields).

## Error handling

- Missing set or missing inventory on either side → `ResourceNotFoundException`
  → 404 `{message}` via `GlobalExceptionHandler`. Message names which set
  number was not found.
- Missing/blank required params (`a`, `b`) → Spring `MissingServletRequest
  Parameter` → 400 via the handler.
- Invalid `category` value → enum bind failure → 400.

## Security

`/api/v1/sets/compare` must be publicly readable (no Bearer). Verify
`SecurityConfig` permits catalog `GET /api/v1/sets/**` reads and that this path
is not swept into the authenticated matcher used by `/{setNumber}/missing-parts`
(which is explicitly authenticated). Add a permit rule for the compare path if
the current config does not already cover it.

## Testing

- **Service unit** (JUnit 5 + Mockito): overlap math, `ONLY_A`/`ONLY_B`/`BOTH`
  categorization, similarity edges (identical inventories → `1.0`, disjoint →
  `0.0`, partial → weighted value), category filter, pagination boundaries,
  same-part-summed-across-rows.
- **Controller** (`@WebMvcTest`, `@AutoConfigureMockMvc(addFilters=false)`):
  param binding, 404 propagation, category filter passthrough, default paging.
- **Integration** (`@SpringBootTest`, real Postgres on `localhost:5433`): seed
  two synthetic sets with overlapping and unique non-spare parts (plus a spare
  row to prove exclusion), assert `similarityScore`, line categories, and
  summary counts through the real JPQL + entity graph.

## Verification

```bash
cd apps/api
nc -z -w2 localhost 5433
./mvnw -Dtest='SetComparison*Test' test 2>&1 | grep -E "Tests run:|BUILD"
./mvnw clean verify 2>&1 | grep -E "Tests run:|BUILD"
```

## Follow-up slices (not this spec)

- Frontend compare page (`/compare?a=&b=`): side-by-side header, similarity
  meter, category-filtered diff table + pagination. Mirrors the missing-pieces
  panel idiom.
- Update `docs/api/openapi.yaml`, Postman collection, and Phase 4 roadmap entry.
