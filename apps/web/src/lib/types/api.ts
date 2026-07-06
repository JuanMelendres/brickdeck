/**
 * Frontend-facing API types.
 *
 * DTO shapes are derived from the backend OpenAPI spec via the generated
 * `schema.d.ts` (run `npm run gen:api` against the running API to refresh).
 * `PageResponse<T>` stays hand-written because OpenAPI cannot express the
 * generic container — the backend emits concrete `PageResponse<Foo>` schemas.
 */
import type { components } from "./schema";

type Schemas = components["schemas"];

/**
 * The backend DTOs are Java records with no OpenAPI nullability metadata, so
 * the generated fields are optional. Jackson serializes absent values as
 * `null`, so model every field as nullable to match the wire reality.
 */
type Nullable<T> = { [K in keyof T]: T[K] | null };

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type CacheStatus =
  | "LOCAL_CACHE_HIT"
  | "IMPORTED_FROM_REBRICKABLE"
  | "EXTERNAL_SEARCH_RESULT";

/** Backend BrickSetResponse, with the friendlier CacheStatus union. */
export type BrickSetResponse = Omit<
  Nullable<Schemas["BrickSetResponse"]>,
  "cacheStatus"
> & {
  cacheStatus: CacheStatus | null;
};

export type SetPartResponse = Nullable<Schemas["SetPartResponse"]>;

export type InventoryImportResult = Nullable<Schemas["InventoryImportResult"]>;
