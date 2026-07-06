import { useQuery } from "@tanstack/react-query";
import { searchSets } from "@/lib/api/sets";
import { queryKeys } from "@/lib/query/keys";

export function useSetSearch(query: string, page: number, size: number) {
  const trimmed = query.trim();
  return useQuery({
    queryKey: queryKeys.sets.search(trimmed, page, size),
    queryFn: () => searchSets(trimmed, page, size),
    enabled: trimmed.length > 0,
  });
}
