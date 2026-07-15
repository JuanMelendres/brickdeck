import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  useAddPriceSnapshot,
  useDeletePriceSnapshot,
  usePriceAnalysis,
} from "./pricingHooks";
import * as api from "@/lib/api/pricing";
import type { PriceAnalysisResponse, PriceSnapshotResponse } from "@/lib/types/pricing";

const analysis = { setNumber: "75257-1", currency: "USD" } as PriceAnalysisResponse;

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

describe("pricing hooks", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("usePriceAnalysis fetches with currency and candidate price", async () => {
    const spy = vi.spyOn(api, "getPriceAnalysis").mockResolvedValue(analysis);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(
      () => usePriceAnalysis("75257-1", { currency: "USD", candidatePrice: 80 }, true),
      { wrapper },
    );

    await waitFor(() => expect(result.current.data).toEqual(analysis));
    expect(spy).toHaveBeenCalledWith("75257-1", { currency: "USD", candidatePrice: 80 });
  });

  it("usePriceAnalysis is disabled when not enabled", () => {
    const spy = vi.spyOn(api, "getPriceAnalysis").mockResolvedValue(analysis);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(
      () => usePriceAnalysis("75257-1", { currency: "USD" }, false),
      { wrapper },
    );

    expect(result.current.fetchStatus).toBe("idle");
    expect(spy).not.toHaveBeenCalled();
  });

  it("useAddPriceSnapshot invalidates pricing queries on success", async () => {
    vi.spyOn(api, "addPriceSnapshot").mockResolvedValue({} as PriceSnapshotResponse);
    const { client, wrapper } = makeWrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useAddPriceSnapshot(), { wrapper });
    result.current.mutate({
      setNumber: "75257-1",
      amount: 100,
      currency: "USD",
      condition: "NEW",
      observedAt: "2026-01-10",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["pricing"] });
  });

  it("useDeletePriceSnapshot invalidates pricing queries on success", async () => {
    vi.spyOn(api, "deletePriceSnapshot").mockResolvedValue(undefined);
    const { client, wrapper } = makeWrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useDeletePriceSnapshot(), { wrapper });
    result.current.mutate("s1");

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["pricing"] });
  });
});
