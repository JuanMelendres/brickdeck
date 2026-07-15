import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import {
  createAlertRule,
  deleteAlertRule,
  dismissTriggeredAlert,
  listAlertRules,
  listTriggeredAlerts,
} from "./alerts";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddPriceAlertRuleRequest,
  PriceAlertRuleResponse,
  TriggeredAlertResponse,
} from "@/lib/types/alerts";

describe("alerts api", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs a new rule", async () => {
    const rule = { id: "r1" } as PriceAlertRuleResponse;
    const spy = vi.spyOn(client, "apiPost").mockResolvedValue(rule);
    const req: AddPriceAlertRuleRequest = {
      setNumber: "75257-1",
      currency: "USD",
      type: "BELOW_TARGET_PRICE",
      thresholdValue: 100,
    };

    const result = await createAlertRule(req);

    expect(spy).toHaveBeenCalledWith("/api/v1/price-alerts", req);
    expect(result).toBe(rule);
  });

  it("GETs rules paginated", async () => {
    const page = { content: [] } as unknown as PageResponse<PriceAlertRuleResponse>;
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    await listAlertRules({ page: 1, size: 10 });

    expect(spy).toHaveBeenCalledWith("/api/v1/price-alerts", { page: 1, size: 10 });
  });

  it("DELETEs a rule", async () => {
    const spy = vi.spyOn(client, "apiDelete").mockResolvedValue(undefined);
    await deleteAlertRule("r1");
    expect(spy).toHaveBeenCalledWith("/api/v1/price-alerts/r1");
  });

  it("GETs triggered alerts paginated", async () => {
    const page = { content: [] } as unknown as PageResponse<TriggeredAlertResponse>;
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    await listTriggeredAlerts({ page: 0, size: 20 });

    expect(spy).toHaveBeenCalledWith("/api/v1/price-alerts/triggered", {
      page: 0,
      size: 20,
    });
  });

  it("DELETEs a triggered alert", async () => {
    const spy = vi.spyOn(client, "apiDelete").mockResolvedValue(undefined);
    await dismissTriggeredAlert("t1");
    expect(spy).toHaveBeenCalledWith("/api/v1/price-alerts/triggered/t1");
  });
});
