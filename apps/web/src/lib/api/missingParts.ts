import { apiGet } from "./client";
import type { MissingPartsReport } from "@/lib/types/missingParts";

export interface MissingPartsParams {
  missingOnly?: boolean;
  page?: number;
  size?: number;
}

/** Fetch the missing-pieces report for a target set (requires auth). */
export function getMissingParts(
  setNumber: string,
  params: MissingPartsParams = {},
): Promise<MissingPartsReport> {
  return apiGet<MissingPartsReport>(
    `/api/v1/sets/${setNumber}/missing-parts`,
    {
      missingOnly: params.missingOnly,
      page: params.page,
      size: params.size,
    },
  );
}
