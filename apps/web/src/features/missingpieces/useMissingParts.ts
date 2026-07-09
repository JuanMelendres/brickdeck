import { useQuery } from "@tanstack/react-query";
import { getMissingParts } from "@/lib/api/missingParts";
import { queryKeys } from "@/lib/query/keys";

/**
 * Fetch the missing-pieces report for a set. Disabled until enabled (e.g. the
 * user is authenticated) since the endpoint requires a Bearer token.
 */
export function useMissingParts(setNumber: string, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.sets.missingParts(setNumber),
    queryFn: () => getMissingParts(setNumber),
    enabled: enabled && setNumber.length > 0,
  });
}
