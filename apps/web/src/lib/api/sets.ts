import { apiGet } from "./client";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";

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
