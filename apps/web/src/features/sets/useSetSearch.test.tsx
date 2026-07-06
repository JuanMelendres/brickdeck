import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useSetSearch } from "./useSetSearch";
import * as setsApi from "@/lib/api/sets";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";

const emptyPage: PageResponse<BrickSetResponse> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("useSetSearch", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("does not query while the search term is empty", () => {
    const spy = vi.spyOn(setsApi, "searchSets").mockResolvedValue(emptyPage);

    const { result } = renderHook(() => useSetSearch("", 0, 20), { wrapper });

    expect(spy).not.toHaveBeenCalled();
    expect(result.current.isFetching).toBe(false);
  });

  it("queries searchSets when a term is present and returns its data", async () => {
    const spy = vi.spyOn(setsApi, "searchSets").mockResolvedValue(emptyPage);

    const { result } = renderHook(() => useSetSearch("x-wing", 0, 20), {
      wrapper,
    });

    await waitFor(() => expect(result.current.data).toEqual(emptyPage));
    expect(spy).toHaveBeenCalledWith("x-wing", 0, 20);
  });
});
