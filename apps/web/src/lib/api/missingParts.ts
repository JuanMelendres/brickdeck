import { apiGet } from "./client";
import type { MissingPartsReport } from "@/lib/types/missingParts";

/** Fetch the missing-pieces report for a target set (requires auth). */
export function getMissingParts(setNumber: string): Promise<MissingPartsReport> {
  return apiGet<MissingPartsReport>(
    `/api/v1/sets/${setNumber}/missing-parts`,
  );
}
