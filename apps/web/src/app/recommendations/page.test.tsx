import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import RecommendationsPage from "./page";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  }),
}));

const { useBuildableRecommendationsMock } = vi.hoisted(() => ({
  useBuildableRecommendationsMock: vi.fn(),
}));

vi.mock("@/features/recommendations/useBuildableRecommendations", () => ({
  useBuildableRecommendations: useBuildableRecommendationsMock,
}));

function pageOf(content: BuildableSetRecommendation[]): PageResponse<BuildableSetRecommendation> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true,
  };
}

describe("RecommendationsPage", () => {
  afterEach(() => vi.clearAllMocks());

  it("renders the heading and a recommendation row", () => {
    useBuildableRecommendationsMock.mockReturnValue({
      data: pageOf([
        {
          setNumber: "100-1",
          name: "Small Set",
          themeName: "City",
          totalRequired: 2,
          totalOwned: 2,
          totalMissing: 0,
          completionPercentage: 100,
          buildable: true,
        },
      ]),
      isLoading: false,
      isError: false,
      error: null,
    });

    render(<RecommendationsPage />);

    expect(
      screen.getByRole("heading", { name: /build recommendations/i }),
    ).toBeInTheDocument();
    expect(screen.getByText("Small Set")).toBeInTheDocument();
    expect(screen.getByText("Buildable")).toBeInTheDocument();
  });
});
