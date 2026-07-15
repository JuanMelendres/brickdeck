import { apiDelete, apiGet, apiPost } from "./client";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddPriceAlertRuleRequest,
  PriceAlertRuleResponse,
  TriggeredAlertResponse,
} from "@/lib/types/alerts";

export interface PageParams {
  page?: number;
  size?: number;
}

/** Create a price-alert rule on a wishlist set (requires auth). */
export function createAlertRule(
  request: AddPriceAlertRuleRequest,
): Promise<PriceAlertRuleResponse> {
  return apiPost<PriceAlertRuleResponse>("/api/v1/price-alerts", request);
}

/** List the user's alert rules. */
export function listAlertRules(
  params: PageParams = {},
): Promise<PageResponse<PriceAlertRuleResponse>> {
  return apiGet<PageResponse<PriceAlertRuleResponse>>("/api/v1/price-alerts", {
    page: params.page,
    size: params.size,
  });
}

/** Delete an alert rule by id. */
export function deleteAlertRule(id: string): Promise<void> {
  return apiDelete(`/api/v1/price-alerts/${id}`);
}

/** List the user's triggered alerts, newest first. */
export function listTriggeredAlerts(
  params: PageParams = {},
): Promise<PageResponse<TriggeredAlertResponse>> {
  return apiGet<PageResponse<TriggeredAlertResponse>>(
    "/api/v1/price-alerts/triggered",
    { page: params.page, size: params.size },
  );
}

/** Dismiss a triggered alert by id. */
export function dismissTriggeredAlert(id: string): Promise<void> {
  return apiDelete(`/api/v1/price-alerts/triggered/${id}`);
}
