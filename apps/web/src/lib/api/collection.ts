import { apiDelete, apiGet, apiPatch, apiPost } from "./client";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddUserPartRequest,
  AddUserSetRequest,
  UpdateUserSetRequest,
  UserPartResponse,
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

/** Partially update a collection set by id (status/price/date). */
export function updateCollectionSet(
  id: string,
  request: UpdateUserSetRequest,
): Promise<UserSetResponse> {
  return apiPatch<UserSetResponse>(`/api/v1/collection/sets/${id}`, request);
}

/** Remove a collection set by id. */
export function removeCollectionSet(id: string): Promise<void> {
  return apiDelete(`/api/v1/collection/sets/${id}`);
}

/** List the authenticated user's loose parts (paginated). */
export function listCollectionParts(
  page: number,
  size: number,
): Promise<PageResponse<UserPartResponse>> {
  return apiGet<PageResponse<UserPartResponse>>("/api/v1/collection/parts", {
    page,
    size,
  });
}

/** Add a loose part to the collection. */
export function addCollectionPart(
  request: AddUserPartRequest,
): Promise<UserPartResponse> {
  return apiPost<UserPartResponse>("/api/v1/collection/parts", request);
}

/** Remove a loose part by id. */
export function removeCollectionPart(id: string): Promise<void> {
  return apiDelete(`/api/v1/collection/parts/${id}`);
}
