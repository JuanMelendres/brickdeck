import { apiGet } from "./client";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

export interface BuildableRecommendationsParams {
  buildableOnly?: boolean;
  page?: number;
  size?: number;
}

/** Fetch the user's buildable wishlist-set recommendations (requires auth). */
export function getBuildableRecommendations(
  params: BuildableRecommendationsParams = {},
): Promise<PageResponse<BuildableSetRecommendation>> {
  return apiGet<PageResponse<BuildableSetRecommendation>>(
    "/api/v1/recommendations/buildable",
    {
      buildableOnly: params.buildableOnly,
      page: params.page,
      size: params.size,
    },
  );
}
