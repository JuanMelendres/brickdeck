import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useSetDetail } from "./useSetDetail";
import { useSetParts } from "./useSetParts";
import { useImportInventory } from "./useImportInventory";
import * as setsApi from "@/lib/api/sets";
import type {
  BrickSetResponse,
  PageResponse,
  SetPartResponse,
} from "@/lib/types/api";

const set = { externalSetNumber: "75257-1", name: "Falcon" } as BrickSetResponse;
const partsPage: PageResponse<SetPartResponse> = {
  content: [],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

describe("useSetDetail", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("fetches the set by number", async () => {
    const spy = vi.spyOn(setsApi, "getSetByNumber").mockResolvedValue(set);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(() => useSetDetail("75257-1"), { wrapper });

    await waitFor(() => expect(result.current.data).toEqual(set));
    expect(spy).toHaveBeenCalledWith("75257-1");
  });
});

describe("useSetParts", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("fetches the paginated parts inventory", async () => {
    const spy = vi.spyOn(setsApi, "getSetParts").mockResolvedValue(partsPage);
    const { wrapper } = makeWrapper();

    const { result } = renderHook(() => useSetParts("75257-1", 0, 50), {
      wrapper,
    });

    await waitFor(() => expect(result.current.data).toEqual(partsPage));
    expect(spy).toHaveBeenCalledWith("75257-1", 0, 50);
  });
});

describe("useImportInventory", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("imports inventory and invalidates the set's parts queries", async () => {
    vi.spyOn(setsApi, "importSetInventory").mockResolvedValue({
      setNumber: "75257-1",
      linesProcessed: 42,
    });
    const { client, wrapper } = makeWrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useImportInventory("75257-1"), {
      wrapper,
    });

    result.current.mutate();

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: ["sets", "parts", "75257-1"],
    });
  });
});
