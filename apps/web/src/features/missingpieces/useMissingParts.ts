import { useQuery } from "@tanstack/react-query";
import { getMissingParts } from "@/lib/api/missingParts";
import { queryKeys } from "@/lib/query/keys";

const PAGE_SIZE = 50;

interface UseMissingPartsOptions {
  missingOnly?: boolean;
  page?: number;
}

/**
 * Fetch the missing-pieces report for a set. Disabled until enabled (e.g. the
 * user is authenticated) since the endpoint requires a Bearer token.
 */
export function useMissingParts(
  setNumber: string,
  enabled: boolean,
  { missingOnly = false, page = 0 }: UseMissingPartsOptions = {},
) {
  return useQuery({
    queryKey: queryKeys.sets.missingParts(setNumber, missingOnly, page),
    queryFn: () =>
      getMissingParts(setNumber, { missingOnly, page, size: PAGE_SIZE }),
    enabled: enabled && setNumber.length > 0,
  });
}
