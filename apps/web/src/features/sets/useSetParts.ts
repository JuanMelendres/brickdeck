import { useQuery } from "@tanstack/react-query";
import { getSetParts } from "@/lib/api/sets";
import { queryKeys } from "@/lib/query/keys";

export function useSetParts(setNumber: string, page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.sets.parts(setNumber, page, size),
    queryFn: () => getSetParts(setNumber, page, size),
    enabled: setNumber.length > 0,
  });
}
