import { describe, expect, it, vi, beforeEach } from "vitest";
import { searchSets } from "./sets";
import * as client from "./client";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";

describe("searchSets", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("calls /api/v1/sets/search with q, page and size", async () => {
    const page: PageResponse<BrickSetResponse> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    const result = await searchSets("millennium", 2, 20);

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/search", {
      q: "millennium",
      page: 2,
      size: 20,
    });
    expect(result).toBe(page);
  });
});
