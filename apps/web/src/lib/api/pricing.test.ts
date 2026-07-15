import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import {
  addPriceSnapshot,
  deletePriceSnapshot,
  getPriceAnalysis,
  listPriceSnapshots,
} from "./pricing";
import type { PageResponse } from "@/lib/types/api";
import type {
  AddPriceSnapshotRequest,
  PriceAnalysisResponse,
  PriceSnapshotResponse,
} from "@/lib/types/pricing";

const snapshot = { id: "s1", setNumber: "75257-1" } as PriceSnapshotResponse;

describe("pricing api", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs a new snapshot", async () => {
    const spy = vi.spyOn(client, "apiPost").mockResolvedValue(snapshot);
    const req: AddPriceSnapshotRequest = {
      setNumber: "75257-1",
      amount: 129.99,
      currency: "USD",
      condition: "NEW",
      observedAt: "2026-01-10",
    };

    const result = await addPriceSnapshot(req);

    expect(spy).toHaveBeenCalledWith("/api/v1/price-snapshots", req);
    expect(result).toBe(snapshot);
  });

  it("GETs snapshots filtered by set number", async () => {
    const page = { content: [] } as unknown as PageResponse<PriceSnapshotResponse>;
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    await listPriceSnapshots({ setNumber: "75257-1", page: 1, size: 10 });

    expect(spy).toHaveBeenCalledWith("/api/v1/price-snapshots", {
      setNumber: "75257-1",
      page: 1,
      size: 10,
    });
  });

  it("DELETEs a snapshot", async () => {
    const spy = vi.spyOn(client, "apiDelete").mockResolvedValue(undefined);

    await deletePriceSnapshot("s1");

    expect(spy).toHaveBeenCalledWith("/api/v1/price-snapshots/s1");
  });

  it("GETs price analysis with currency and candidate price", async () => {
    const analysis = { setNumber: "75257-1" } as PriceAnalysisResponse;
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(analysis);

    const result = await getPriceAnalysis("75257-1", {
      currency: "USD",
      candidatePrice: 119.99,
    });

    expect(spy).toHaveBeenCalledWith("/api/v1/sets/75257-1/price-analysis", {
      currency: "USD",
      candidatePrice: 119.99,
    });
    expect(result).toBe(analysis);
  });
});
