import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  useAlertRules,
  useCreateAlertRule,
  useDismissTriggeredAlert,
  useTriggeredAlerts,
} from "./alertsHooks";
import * as api from "@/lib/api/alerts";
import type { PageResponse } from "@/lib/types/api";
import type { PriceAlertRuleResponse } from "@/lib/types/alerts";

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

describe("alerts hooks", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("useAlertRules fetches when enabled", async () => {
    const page = { content: [] } as unknown as PageResponse<PriceAlertRuleResponse>;
    const spy = vi.spyOn(api, "listAlertRules").mockResolvedValue(page);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(() => useAlertRules(true, 0), { wrapper });

    await waitFor(() => expect(result.current.data).toEqual(page));
    expect(spy).toHaveBeenCalledWith({ page: 0, size: 20 });
  });

  it("useTriggeredAlerts is disabled when not enabled", () => {
    const spy = vi.spyOn(api, "listTriggeredAlerts");
    const { wrapper } = makeWrapper();

    const { result } = renderHook(() => useTriggeredAlerts(false, 0), { wrapper });

    expect(result.current.fetchStatus).toBe("idle");
    expect(spy).not.toHaveBeenCalled();
  });

  it("useCreateAlertRule invalidates alerts on success", async () => {
    vi.spyOn(api, "createAlertRule").mockResolvedValue({} as PriceAlertRuleResponse);
    const { client, wrapper } = makeWrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useCreateAlertRule(), { wrapper });
    result.current.mutate({
      setNumber: "75257-1",
      currency: "USD",
      type: "BELOW_TARGET_PRICE",
      thresholdValue: 100,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["alerts"] });
  });

  it("useDismissTriggeredAlert invalidates alerts on success", async () => {
    vi.spyOn(api, "dismissTriggeredAlert").mockResolvedValue(undefined);
    const { client, wrapper } = makeWrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useDismissTriggeredAlert(), { wrapper });
    result.current.mutate("t1");

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["alerts"] });
  });
});
