import { useQuery } from "@tanstack/react-query";
import { getBuildableRecommendations } from "@/lib/api/recommendations";
import { queryKeys } from "@/lib/query/keys";

const PAGE_SIZE = 20;

interface UseBuildableRecommendationsOptions {
  buildableOnly?: boolean;
  page?: number;
}

/**
 * Fetch the user's buildable wishlist-set recommendations. Disabled until
 * enabled (e.g. the user is authenticated) since the endpoint requires a
 * Bearer token.
 */
export function useBuildableRecommendations(
  { buildableOnly = false, page = 0 }: UseBuildableRecommendationsOptions = {},
  enabled = true,
) {
  return useQuery({
    queryKey: queryKeys.recommendations.buildable(buildableOnly, page),
    queryFn: () =>
      getBuildableRecommendations({ buildableOnly, page, size: PAGE_SIZE }),
    enabled,
  });
}
