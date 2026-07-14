/**
 * Set-comparison report types. Hand-written because the generated OpenAPI
 * `schema.d.ts` predates the compare endpoint. Mirrors the backend
 * SetComparisonReport / SetComparisonLine records.
 */
export type ComparisonCategory = "ONLY_A" | "ONLY_B" | "BOTH";

export interface SetComparisonLine {
  partNumber: string | null;
  partName: string | null;
  partImageUrl: string | null;
  colorExternalId: number | null;
  colorName: string | null;
  colorRgb: string | null;
  quantityA: number;
  quantityB: number;
  shared: number;
  category: ComparisonCategory;
}

export interface SetComparisonReport {
  setNumberA: string;
  setNumberB: string;
  /** Quantity-weighted similarity, 0..1 (2 decimal places). */
  similarityScore: number;
  sharedLineCount: number;
  onlyALineCount: number;
  onlyBLineCount: number;
  lines: SetComparisonLine[];
  page: number;
  size: number;
  totalLines: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
