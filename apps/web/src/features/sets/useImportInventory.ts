import { useMutation, useQueryClient } from "@tanstack/react-query";
import { importSetInventory } from "@/lib/api/sets";
import { queryKeys } from "@/lib/query/keys";

export function useImportInventory(setNumber: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => importSetInventory(setNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.sets.partsAll(setNumber),
      });
    },
  });
}
