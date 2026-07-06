import { describe, expect, it, vi, beforeEach } from "vitest";
import { getSetByNumber, getSetParts, importSetInventory, searchSets } from "./sets";
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

describe("getSetByNumber", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("calls /api/v1/sets/by-number/{setNumber}", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue({} as never);

    await getSetByNumber("75257-1");

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/by-number/75257-1");
  });

  it("encodes the set number in the path", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue({} as never);

    await getSetByNumber("75257 1");

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/by-number/75257%201");
  });
});

describe("getSetParts", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("calls /api/v1/sets/{setNumber}/parts with pagination", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue({} as never);

    await getSetParts("75257-1", 1, 50);

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/75257-1/parts", {
      page: 1,
      size: 50,
    });
  });
});

describe("importSetInventory", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("POSTs to the inventory import endpoint", async () => {
    const spy = vi
      .spyOn(client, "apiPost")
      .mockResolvedValue({ setNumber: "75257-1", linesProcessed: 42 });

    const result = await importSetInventory("75257-1");

    expect(spy).toHaveBeenCalledWith(
      "/api/v1/catalog/sets/75257-1/inventory/import",
    );
    expect(result.linesProcessed).toBe(42);
  });
});
