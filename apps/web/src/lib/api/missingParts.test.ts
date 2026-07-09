import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { getMissingParts } from "./missingParts";
import type { MissingPartsReport } from "@/lib/types/missingParts";

describe("getMissingParts", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the missing-parts report for a set", async () => {
    const report: MissingPartsReport = {
      setNumber: "75257-1",
      totalRequired: 6,
      totalOwned: 4,
      totalMissing: 2,
      completionPercentage: 66.7,
      lines: [],
    };
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(report);

    const result = await getMissingParts("75257-1");

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/75257-1/missing-parts");
    expect(result).toBe(report);
  });
});
