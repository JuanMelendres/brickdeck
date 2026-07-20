import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useBuildableRecommendations } from "./useBuildableRecommendations";
import * as api from "@/lib/api/recommendations";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

const page = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
} as PageResponse<BuildableSetRecommendation>;

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper };
}

describe("useBuildableRecommendations", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("fetches with buildableOnly and page", async () => {
    const spy = vi
      .spyOn(api, "getBuildableRecommendations")
      .mockResolvedValue(page);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(
      () => useBuildableRecommendations({ buildableOnly: true, page: 2 }),
      { wrapper },
    );

    await waitFor(() => expect(result.current.data).toEqual(page));
    expect(spy).toHaveBeenCalledWith({ buildableOnly: true, page: 2, size: 20 });
  });

  it("is disabled when enabled is false", () => {
    const spy = vi
      .spyOn(api, "getBuildableRecommendations")
      .mockResolvedValue(page);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(
      () => useBuildableRecommendations({}, false),
      { wrapper },
    );

    expect(result.current.fetchStatus).toBe("idle");
    expect(spy).not.toHaveBeenCalled();
  });
});
