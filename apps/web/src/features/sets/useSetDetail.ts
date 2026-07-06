import { useQuery } from "@tanstack/react-query";
import { getSetByNumber } from "@/lib/api/sets";
import { queryKeys } from "@/lib/query/keys";

export function useSetDetail(setNumber: string) {
  return useQuery({
    queryKey: queryKeys.sets.detail(setNumber),
    queryFn: () => getSetByNumber(setNumber),
    enabled: setNumber.length > 0,
  });
}
