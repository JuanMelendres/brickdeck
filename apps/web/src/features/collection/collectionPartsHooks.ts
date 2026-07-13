import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addCollectionPart,
  listCollectionParts,
  removeCollectionPart,
  updateCollectionPart,
} from "@/lib/api/collection";
import { queryKeys } from "@/lib/query/keys";
import type {
  AddUserPartRequest,
  UpdateUserPartRequest,
} from "@/lib/types/collection";

export function useCollectionParts(page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.collection.parts(page, size),
    queryFn: () => listCollectionParts(page, size),
  });
}

export function useAddCollectionPart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AddUserPartRequest) => addCollectionPart(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.partsAll });
    },
  });
}

export function useUpdateCollectionPart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: string;
      request: UpdateUserPartRequest;
    }) => updateCollectionPart(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.partsAll });
    },
  });
}

export function useRemoveCollectionPart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => removeCollectionPart(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection.partsAll });
    },
  });
}
