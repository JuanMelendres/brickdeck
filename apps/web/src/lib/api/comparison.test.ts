import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { getSetComparison } from "./comparison";
import type { SetComparisonReport } from "@/lib/types/comparison";

const report = { setNumberA: "75257-1", setNumberB: "10300-1" } as SetComparisonReport;

describe("getSetComparison", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the compare endpoint with both set numbers and default (empty) params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(report);

    const result = await getSetComparison("75257-1", "10300-1");

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/compare", {
      a: "75257-1",
      b: "10300-1",
      category: undefined,
      page: undefined,
      size: undefined,
    });
    expect(result).toBe(report);
  });

  it("passes category and pagination params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(report);

    await getSetComparison("75257-1", "10300-1", {
      category: "BOTH",
      page: 1,
      size: 25,
    });

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/compare", {
      a: "75257-1",
      b: "10300-1",
      category: "BOTH",
      page: 1,
      size: 25,
    });
  });
});
