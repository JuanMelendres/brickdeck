import { apiDelete, apiGet, apiPost } from "./client";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddPriceSnapshotRequest,
  PriceAnalysisResponse,
  PriceSnapshotResponse,
} from "@/lib/types/pricing";

/** Record an observed price for a set (requires auth). */
export function addPriceSnapshot(
  request: AddPriceSnapshotRequest,
): Promise<PriceSnapshotResponse> {
  return apiPost<PriceSnapshotResponse>("/api/v1/price-snapshots", request);
}

export interface ListPriceSnapshotsParams {
  setNumber?: string;
  page?: number;
  size?: number;
}

/** List the user's price snapshots, optionally filtered by set. */
export function listPriceSnapshots(
  params: ListPriceSnapshotsParams = {},
): Promise<PageResponse<PriceSnapshotResponse>> {
  return apiGet<PageResponse<PriceSnapshotResponse>>("/api/v1/price-snapshots", {
    setNumber: params.setNumber,
    page: params.page,
    size: params.size,
  });
}

/** Delete a price snapshot by id. */
export function deletePriceSnapshot(id: string): Promise<void> {
  return apiDelete(`/api/v1/price-snapshots/${id}`);
}

export interface PriceAnalysisParams {
  currency: string;
  candidatePrice?: number;
}

/** Price analysis + optional deal verdict for a set in one currency. */
export function getPriceAnalysis(
  setNumber: string,
  params: PriceAnalysisParams,
): Promise<PriceAnalysisResponse> {
  return apiGet<PriceAnalysisResponse>(
    `/api/v1/sets/${setNumber}/price-analysis`,
    { currency: params.currency, candidatePrice: params.candidatePrice },
  );
}
