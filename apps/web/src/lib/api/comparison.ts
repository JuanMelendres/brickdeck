import { apiGet } from "./client";
import type {
  ComparisonCategory,
  SetComparisonReport,
} from "@/lib/types/comparison";

export interface ComparisonParams {
  category?: ComparisonCategory;
  page?: number;
  size?: number;
}

/** Compare two catalog sets' inventories (public endpoint, no auth). */
export function getSetComparison(
  a: string,
  b: string,
  params: ComparisonParams = {},
): Promise<SetComparisonReport> {
  return apiGet<SetComparisonReport>("/api/v1/sets/compare", {
    a,
    b,
    category: params.category,
    page: params.page,
    size: params.size,
  });
}
