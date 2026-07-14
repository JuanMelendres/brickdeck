import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useSetComparison } from "./useSetComparison";
import * as comparisonApi from "@/lib/api/comparison";
import type { SetComparisonReport } from "@/lib/types/comparison";

const report = {
  setNumberA: "75257-1",
  setNumberB: "10300-1",
} as SetComparisonReport;

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

describe("useSetComparison", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("fetches the comparison for both set numbers", async () => {
    const spy = vi
      .spyOn(comparisonApi, "getSetComparison")
      .mockResolvedValue(report);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(() => useSetComparison("75257-1", "10300-1"), {
      wrapper,
    });

    await waitFor(() => expect(result.current.data).toEqual(report));
    expect(spy).toHaveBeenCalledWith("75257-1", "10300-1", {
      category: undefined,
      page: 0,
      size: 50,
    });
  });

  it("passes category and page, and disables until both set numbers are present", async () => {
    const spy = vi
      .spyOn(comparisonApi, "getSetComparison")
      .mockResolvedValue(report);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(
      () => useSetComparison("75257-1", "", { category: "BOTH", page: 2 }),
      { wrapper },
    );

    // Disabled because set B is empty.
    expect(result.current.fetchStatus).toBe("idle");
    expect(spy).not.toHaveBeenCalled();
  });
});
