import { useQuery } from "@tanstack/react-query";
import { getSetComparison } from "@/lib/api/comparison";
import { queryKeys } from "@/lib/query/keys";
import type { ComparisonCategory } from "@/lib/types/comparison";

const PAGE_SIZE = 50;

interface UseSetComparisonOptions {
  category?: ComparisonCategory;
  page?: number;
}

/**
 * Compare two catalog sets. Disabled until both set numbers are present.
 * The compare endpoint is public, so no auth gating is needed.
 */
export function useSetComparison(
  a: string,
  b: string,
  { category, page = 0 }: UseSetComparisonOptions = {},
) {
  return useQuery({
    queryKey: queryKeys.sets.comparison(a, b, category ?? null, page),
    queryFn: () => getSetComparison(a, b, { category, page, size: PAGE_SIZE }),
    enabled: a.length > 0 && b.length > 0,
  });
}
