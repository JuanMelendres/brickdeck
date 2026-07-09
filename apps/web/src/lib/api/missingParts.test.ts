import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { getMissingParts } from "./missingParts";
import type { MissingPartsReport } from "@/lib/types/missingParts";

const report = { setNumber: "75257-1" } as MissingPartsReport;

describe("getMissingParts", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the report with default (empty) params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(report);

    const result = await getMissingParts("75257-1");

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/75257-1/missing-parts", {
      missingOnly: undefined,
      page: undefined,
      size: undefined,
    });
    expect(result).toBe(report);
  });

  it("passes missingOnly and pagination params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(report);

    await getMissingParts("75257-1", { missingOnly: true, page: 1, size: 25 });

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/75257-1/missing-parts", {
      missingOnly: true,
      page: 1,
      size: 25,
    });
  });
});
