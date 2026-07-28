/**
 * Classification API contract types. Hand-written because the backend OpenAPI
 * schema (schema.d.ts) predates this endpoint. Mirrors the backend records:
 * PartResolutionStatus / PartSuggestion / ColorSuggestion / PartClassificationResponse.
 */
export type PartResolutionStatus = "RESOLVED" | "UNRESOLVED";

export interface PartSuggestion {
  partNumber: string;
  partNameGuess: string | null;
  score: number | null;
  resolutionStatus: PartResolutionStatus;
  referenceImageUrl: string | null;
}

export interface ColorSuggestion {
  /** Null when the guessed color name isn't in the local catalog. */
  colorId: number | null;
  colorName: string;
  confidence: number | null;
}

export interface PartClassificationResponse {
  colorSuggestion: ColorSuggestion;
  partSuggestions: PartSuggestion[];
}
