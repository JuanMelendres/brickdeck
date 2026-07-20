import { describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RecommendationsView } from "./RecommendationsView";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

const buildableRec: BuildableSetRecommendation = {
  setNumber: "100-1",
  name: "Small Set",
  themeName: "City",
  totalRequired: 2,
  totalOwned: 2,
  totalMissing: 0,
  completionPercentage: 100,
  buildable: true,
};

const partialRec: BuildableSetRecommendation = {
  setNumber: "200-1",
  name: "Big Set",
  themeName: "Technic",
  totalRequired: 10,
  totalOwned: 4,
  totalMissing: 6,
  completionPercentage: 40,
  buildable: false,
};

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

function baseProps() {
  return {
    buildableOnly: false,
    onBuildableOnlyChange: vi.fn(),
    onPageChange: vi.fn(),
    isLoading: false,
    isError: false,
    error: undefined as unknown,
    data: pageOf([buildableRec, partialRec]),
  };
}

describe("RecommendationsView", () => {
  it("renders a row per recommendation with set number and completion", () => {
    render(<RecommendationsView {...baseProps()} />);

    const row = screen.getByText("Small Set").closest("tr") as HTMLElement;
    const cells = within(row);
    expect(cells.getByText("100-1")).toBeInTheDocument();
    expect(cells.getByText("City")).toBeInTheDocument();
    expect(cells.getByText(/100%/)).toBeInTheDocument();
  });

  it("marks a buildable set with a Buildable badge", () => {
    render(<RecommendationsView {...baseProps()} />);
    const row = screen.getByText("Small Set").closest("tr") as HTMLElement;
    expect(within(row).getByText(/buildable/i)).toBeInTheDocument();
  });

  it("toggles the buildable-only filter", async () => {
    const props = baseProps();
    render(<RecommendationsView {...props} />);

    await userEvent.click(screen.getByRole("switch", { name: /buildable only/i }));

    expect(props.onBuildableOnlyChange).toHaveBeenCalledWith(true);
  });

  it("shows a loading indicator", () => {
    render(<RecommendationsView {...baseProps()} isLoading data={undefined} />);
    expect(screen.getByLabelText(/loading/i)).toBeInTheDocument();
  });

  it("shows an empty state when there are no recommendations", () => {
    render(<RecommendationsView {...baseProps()} data={pageOf([])} />);
    expect(screen.getByText(/no wishlist sets/i)).toBeInTheDocument();
  });

  it("shows an error state", () => {
    render(
      <RecommendationsView
        {...baseProps()}
        isError
        error={new Error("boom")}
        data={undefined}
      />,
    );
    expect(screen.getByText(/boom/i)).toBeInTheDocument();
  });
});
