import { apiGet, apiPost } from "./client";
import type {
  BrickSetResponse,
  InventoryImportResult,
  PageResponse,
  SetPartResponse,
} from "@/lib/types/api";

export function searchSets(
  query: string,
  page: number,
  size: number,
): Promise<PageResponse<BrickSetResponse>> {
  return apiGet<PageResponse<BrickSetResponse>>("/api/v1/sets/search", {
    q: query,
    page,
    size,
  });
}

export function getSetByNumber(setNumber: string): Promise<BrickSetResponse> {
  return apiGet<BrickSetResponse>(
    `/api/v1/sets/by-number/${encodeURIComponent(setNumber)}`,
  );
}

export function getSetParts(
  setNumber: string,
  page: number,
  size: number,
): Promise<PageResponse<SetPartResponse>> {
  return apiGet<PageResponse<SetPartResponse>>(
    `/api/v1/sets/${encodeURIComponent(setNumber)}/parts`,
    { page, size },
  );
}

export function importSetInventory(
  setNumber: string,
): Promise<InventoryImportResult> {
  return apiPost<InventoryImportResult>(
    `/api/v1/catalog/sets/${encodeURIComponent(setNumber)}/inventory/import`,
  );
}
