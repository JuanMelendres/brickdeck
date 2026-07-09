/**
 * Missing-pieces report types. Hand-written because the generated OpenAPI
 * `schema.d.ts` predates the missing-parts endpoint. Mirrors the backend
 * MissingPartsReport / MissingPartLine records.
 */
export interface MissingPartLine {
  partNumber: string | null;
  partName: string | null;
  partImageUrl: string | null;
  colorExternalId: number | null;
  colorName: string | null;
  colorRgb: string | null;
  required: number;
  owned: number;
  missing: number;
}

export interface MissingPartsReport {
  setNumber: string;
  totalRequired: number;
  totalOwned: number;
  totalMissing: number;
  completionPercentage: number;
  lines: MissingPartLine[];
}
