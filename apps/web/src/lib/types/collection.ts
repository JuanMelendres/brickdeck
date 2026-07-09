/**
 * Collection API contract types. Hand-written because the backend OpenAPI spec
 * (schema.d.ts) predates the collection endpoints. Mirrors the backend records:
 * CollectionStatus / AddUserSetRequest / UpdateUserSetRequest / UserSetResponse.
 */
export type CollectionStatus = "OWNED" | "WISHLIST" | "BUILT" | "IN_PROGRESS";

export const COLLECTION_STATUSES: CollectionStatus[] = [
  "OWNED",
  "WISHLIST",
  "BUILT",
  "IN_PROGRESS",
];

export interface UserSetResponse {
  id: string;
  setNumber: string | null;
  setName: string | null;
  yearReleased: number | null;
  themeName: string | null;
  imageUrl: string | null;
  status: CollectionStatus | null;
  purchasePrice: number | null;
  purchaseDate: string | null;
}

export interface AddUserSetRequest {
  setNumber: string;
  status?: CollectionStatus;
  purchasePrice?: number;
  purchaseDate?: string;
}

export interface UpdateUserSetRequest {
  status?: CollectionStatus;
  purchasePrice?: number;
  purchaseDate?: string;
}
