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

export interface UserPartResponse {
  id: string;
  partNumber: string | null;
  partName: string | null;
  partImageUrl: string | null;
  colorExternalId: number | null;
  colorName: string | null;
  colorRgb: string | null;
  quantity: number | null;
  storageLocation: string | null;
}

export interface AddUserPartRequest {
  externalPartNumber: string;
  colorExternalId: number;
  quantity: number;
  storageLocation?: string;
}

export interface UpdateUserPartRequest {
  quantity?: number;
  storageLocation?: string;
}
