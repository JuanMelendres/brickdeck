/**
 * Price-alert types. Hand-written because the generated OpenAPI `schema.d.ts`
 * predates the endpoints. Mirrors the backend price-alert DTOs.
 */
export type PriceAlertType =
  | "BELOW_TARGET_PRICE"
  | "PERCENT_BELOW_AVERAGE"
  | "AT_OR_BELOW_LOWEST";

export interface AddPriceAlertRuleRequest {
  setNumber: string;
  currency: string;
  type: PriceAlertType;
  thresholdValue?: number;
}

export interface PriceAlertRuleResponse {
  id: string;
  setNumber: string;
  currency: string;
  type: PriceAlertType;
  thresholdValue: number | null;
  active: boolean;
  createdAt: string;
}

export interface TriggeredAlertResponse {
  id: string;
  ruleId: string;
  setNumber: string;
  amount: number;
  currency: string;
  message: string;
  triggeredAt: string;
}
