import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { getBuildableRecommendations } from "./recommendations";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

const page = { content: [] } as unknown as PageResponse<BuildableSetRecommendation>;

describe("getBuildableRecommendations", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the buildable endpoint with default (empty) params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    const result = await getBuildableRecommendations();

    expect(spy).toHaveBeenCalledWith("/api/v1/recommendations/buildable", {
      buildableOnly: undefined,
      page: undefined,
      size: undefined,
    });
    expect(result).toBe(page);
  });

  it("passes buildableOnly and pagination params", async () => {
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    await getBuildableRecommendations({ buildableOnly: true, page: 1, size: 10 });

    expect(spy).toHaveBeenCalledWith("/api/v1/recommendations/buildable", {
      buildableOnly: true,
      page: 1,
      size: 10,
    });
  });
});
