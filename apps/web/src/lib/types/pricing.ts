/**
 * Pricing types. Hand-written because the generated OpenAPI `schema.d.ts`
 * predates the pricing endpoints. Mirrors the backend pricing DTOs.
 */
export type PriceCondition = "NEW" | "USED";
export type PriceSource = "MANUAL";
export type DealVerdict = "GREAT_DEAL" | "GOOD_DEAL" | "FAIR" | "POOR";

export interface AddPriceSnapshotRequest {
  setNumber: string;
  amount: number;
  currency: string;
  condition: PriceCondition;
  observedAt: string;
  store?: string;
  url?: string;
}

export interface PriceSnapshotResponse {
  id: string;
  setNumber: string;
  amount: number;
  currency: string;
  condition: PriceCondition;
  source: PriceSource;
  observedAt: string;
  store: string | null;
  url: string | null;
  createdAt: string;
}

export interface CandidateEvaluation {
  amount: number;
  pricePerPiece: number | null;
  percentBelowAverage: number;
  atOrBelowLowest: boolean;
  verdict: DealVerdict;
}

export interface PriceAnalysisResponse {
  setNumber: string;
  currency: string;
  snapshotCount: number;
  minAmount: number;
  averageAmount: number;
  maxAmount: number;
  latestAmount: number;
  numberOfParts: number | null;
  pricePerPiece: number | null;
  candidate: CandidateEvaluation | null;
}
