/**
 * Build-recommendation types. Hand-written because the generated OpenAPI
 * `schema.d.ts` predates the endpoint. Mirrors the backend
 * BuildableSetRecommendation record.
 */
export interface BuildableSetRecommendation {
  setNumber: string;
  name: string | null;
  themeName: string | null;
  totalRequired: number;
  totalOwned: number;
  totalMissing: number;
  completionPercentage: number;
  buildable: boolean;
}
