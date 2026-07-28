import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import * as api from "@/lib/api/classification";
import { useClassifyPart } from "./useClassifyPart";
import type { PartClassificationResponse } from "@/lib/types/classification";

function wrapper() {
  const client = new QueryClient({
    defaultOptions: { mutations: { retry: false } },
  });
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    );
  }
  return Wrapper;
}

describe("useClassifyPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("classifies a photo and exposes the response", async () => {
    const response: PartClassificationResponse = {
      colorSuggestion: { colorId: 4, colorName: "Red", confidence: 0.95 },
      partSuggestions: [],
    };
    vi.spyOn(api, "classifyPart").mockResolvedValue(response);
    const file = new File(["fake"], "photo.jpg", { type: "image/jpeg" });

    const { result } = renderHook(() => useClassifyPart(), {
      wrapper: wrapper(),
    });
    result.current.mutate(file);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(response);
    expect(api.classifyPart).toHaveBeenCalledWith(file);
  });
});
