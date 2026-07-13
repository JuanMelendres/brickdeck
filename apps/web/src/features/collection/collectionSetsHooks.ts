import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addCollectionSet,
  listCollectionSets,
  removeCollectionSet,
  updateCollectionSet,
} from "@/lib/api/collection";
import { queryKeys } from "@/lib/query/keys";
import type {
  AddUserSetRequest,
  UpdateUserSetRequest,
} from "@/lib/types/collection";

export function useCollectionSets(page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.collection.sets(page, size),
    queryFn: () => listCollectionSets(page, size),
  });
}

export function useAddCollectionSet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AddUserSetRequest) => addCollectionSet(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.setsAll });
    },
  });
}

export function useUpdateCollectionSet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: string;
      request: UpdateUserSetRequest;
    }) => updateCollectionSet(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.setsAll });
    },
  });
}

export function useRemoveCollectionSet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => removeCollectionSet(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.setsAll });
    },
  });
}
