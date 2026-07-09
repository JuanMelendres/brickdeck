import { apiDelete, apiGet, apiPost } from "./client";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddUserSetRequest,
  UserSetResponse,
} from "@/lib/types/collection";

/** List the authenticated user's collection sets (paginated). */
export function listCollectionSets(
  page: number,
  size: number,
): Promise<PageResponse<UserSetResponse>> {
  return apiGet<PageResponse<UserSetResponse>>("/api/v1/collection/sets", {
    page,
    size,
  });
}

/** Add a set to the collection. */
export function addCollectionSet(
  request: AddUserSetRequest,
): Promise<UserSetResponse> {
  return apiPost<UserSetResponse>("/api/v1/collection/sets", request);
}

/** Remove a collection set by id. */
export function removeCollectionSet(id: string): Promise<void> {
  return apiDelete(`/api/v1/collection/sets/${id}`);
}
