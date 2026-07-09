import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import * as api from "@/lib/api/collection";
import {
  useAddCollectionPart,
  useCollectionParts,
  useRemoveCollectionPart,
} from "./collectionPartsHooks";
import type { PageResponse } from "@/lib/types/api";
import type { UserPartResponse } from "@/lib/types/collection";

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

const emptyPage: PageResponse<UserPartResponse> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

describe("useCollectionParts", () => {
  afterEach(() => vi.restoreAllMocks());

  it("fetches loose parts", async () => {
    vi.spyOn(api, "listCollectionParts").mockResolvedValue(emptyPage);
    const { Wrapper } = wrapper();

    const { result } = renderHook(() => useCollectionParts(0, 20), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(emptyPage);
  });
});

describe("useAddCollectionPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("adds a part and invalidates the parts list", async () => {
    vi.spyOn(api, "addCollectionPart").mockResolvedValue({
      id: "cp1",
    } as UserPartResponse);
    const { client, Wrapper } = wrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useAddCollectionPart(), {
      wrapper: Wrapper,
    });
    await result.current.mutateAsync({
      externalPartNumber: "3001",
      colorExternalId: 4,
      quantity: 10,
    });

    expect(api.addCollectionPart).toHaveBeenCalled();
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: ["collection", "parts"],
    });
  });
});

describe("useRemoveCollectionPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("removes a part and invalidates the parts list", async () => {
    vi.spyOn(api, "removeCollectionPart").mockResolvedValue(undefined);
    const { client, Wrapper } = wrapper();
    const invalidate = vi.spyOn(client, "invalidateQueries");

    const { result } = renderHook(() => useRemoveCollectionPart(), {
      wrapper: Wrapper,
    });
    await result.current.mutateAsync("cp1");

    expect(api.removeCollectionPart).toHaveBeenCalledWith("cp1");
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: ["collection", "parts"],
    });
  });
});
