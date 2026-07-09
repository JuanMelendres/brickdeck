import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import * as api from "@/lib/api/collection";
import {
  useAddCollectionSet,
  useCollectionSets,
  useRemoveCollectionSet,
} from "./collectionSetsHooks";
import type { PageResponse } from "@/lib/types/api";
import type { UserSetResponse } from "@/lib/types/collection";

function wrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return {
    client,
    Wrapper: ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    ),
  };
}

const emptyPage: PageResponse<UserSetResponse> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

describe("useCollectionSets", () => {
  afterEach(() => vi.restoreAllMocks());

  it("fetches collection sets", async () => {
    vi.spyOn(api, "listCollectionSets").mockResolvedValue(emptyPage);
    const { Wrapper } = wrapper();

    const { result } = renderHook(() => useCollectionSets(0, 20), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(emptyPage);
  });
});

describe("useAddCollectionSet", () => {
  afterEach(() => vi.restoreAllMocks());

  it("adds a set and invalidates the collection list", async () => {
    vi.spyOn(api, "addCollectionSet").mockResolvedValue({
      id: "cs1",
    } as UserSetResponse);
    const { client, Wrapper } = wrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useAddCollectionSet(), {
      wrapper: Wrapper,
    });
    await result.current.mutateAsync({ setNumber: "75257-1", status: "OWNED" });

    expect(api.addCollectionSet).toHaveBeenCalled();
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: ["collection", "sets"],
    });
  });
});

describe("useRemoveCollectionSet", () => {
  afterEach(() => vi.restoreAllMocks());

  it("removes a set and invalidates the collection list", async () => {
    vi.spyOn(api, "removeCollectionSet").mockResolvedValue(undefined);
    const { client, Wrapper } = wrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useRemoveCollectionSet(), {
      wrapper: Wrapper,
    });
    await result.current.mutateAsync("cs1");

    expect(api.removeCollectionSet).toHaveBeenCalledWith("cs1");
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: ["collection", "sets"],
    });
  });
});
