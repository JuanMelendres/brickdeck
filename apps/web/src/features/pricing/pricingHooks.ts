import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addPriceSnapshot,
  deletePriceSnapshot,
  getPriceAnalysis,
  listPriceSnapshots,
} from "@/lib/api/pricing";
import { queryKeys } from "@/lib/query/keys";

const SNAPSHOTS_PAGE_SIZE = 20;

interface PriceAnalysisOptions {
  currency: string;
  candidatePrice?: number;
}

/**
 * Price analysis for a set in one currency. Disabled until enabled (the user is
 * authenticated) since the endpoint requires a Bearer token.
 */
export function usePriceAnalysis(
  setNumber: string,
  { currency, candidatePrice }: PriceAnalysisOptions,
  enabled: boolean,
) {
  return useQuery({
    queryKey: queryKeys.pricing.analysis(setNumber, currency, candidatePrice ?? null),
    queryFn: () => getPriceAnalysis(setNumber, { currency, candidatePrice }),
    enabled: enabled && setNumber.length > 0 && currency.length > 0,
  });
}

/** The user's price snapshots for a set (paginated). */
export function usePriceSnapshots(setNumber: string, enabled: boolean, page = 0) {
  return useQuery({
    queryKey: queryKeys.pricing.snapshots(setNumber, page),
    queryFn: () =>
      listPriceSnapshots({ setNumber, page, size: SNAPSHOTS_PAGE_SIZE }),
    enabled: enabled && setNumber.length > 0,
  });
}

export function useAddPriceSnapshot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: addPriceSnapshot,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.pricing.all }),
  });
}

export function useDeletePriceSnapshot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deletePriceSnapshot,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.pricing.all }),
  });
}
