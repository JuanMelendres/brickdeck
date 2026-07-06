/**
 * Types mirroring the BrickDeck backend DTOs (com.brickdeck.api.catalog.dto).
 * Future: generate from the OpenAPI spec at /v3/api-docs.
 */

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

/** Mirrors BrickSetResponse.java. */
export interface BrickSetResponse {
  id: string | null;
  externalSetNumber: string;
  name: string;
  yearReleased: number | null;
  themeId: string | null;
  themeName: string | null;
  externalThemeId: number | null;
  numberOfParts: number | null;
  imageUrl: string | null;
  externalUrl: string | null;
  source: string | null;
  cacheStatus: CacheStatus;
}

/** Mirrors SetPartResponse.java. */
export interface SetPartResponse {
  id: string;
  setNumber: string;
  partNumber: string;
  partName: string;
  partImageUrl: string | null;
  colorExternalId: number | null;
  colorName: string | null;
  colorRgb: string | null;
  quantity: number;
  spare: boolean;
  elementId: string | null;
}

/** Mirrors InventoryImportResult.java. */
export interface InventoryImportResult {
  setNumber: string;
  linesProcessed: number;
}
