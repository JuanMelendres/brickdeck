import { describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SetComparisonView } from "./SetComparisonView";
import { ApiError } from "@/lib/api/client";
import type {
  SetComparisonLine,
  SetComparisonReport,
} from "@/lib/types/comparison";

const line: SetComparisonLine = {
  partNumber: "3001",
  partName: "Brick 2 x 4",
  partImageUrl: null,
  colorExternalId: 4,
  colorName: "Red",
  colorRgb: "C91A09",
  quantityA: 5,
  quantityB: 2,
  shared: 2,
  category: "BOTH",
};

const report: SetComparisonReport = {
  setNumberA: "75257-1",
  setNumberB: "10300-1",
  similarityScore: 0.42,
  sharedLineCount: 1,
  onlyALineCount: 3,
  onlyBLineCount: 2,
  lines: [line],
  page: 0,
  size: 50,
  totalLines: 1,
  totalPages: 1,
  first: true,
  last: true,
};

function baseProps() {
  return {
    category: null,
    onCategoryChange: vi.fn(),
    onPageChange: vi.fn(),
    isLoading: false,
    isError: false,
    error: undefined as unknown,
    report,
  };
}

describe("SetComparisonView", () => {
  it("shows both set numbers and the similarity percentage", () => {
    render(<SetComparisonView {...baseProps()} />);

    expect(screen.getByText("75257-1")).toBeInTheDocument();
    expect(screen.getByText("10300-1")).toBeInTheDocument();
    expect(screen.getByText(/42% similar/i)).toBeInTheDocument();
  });

  it("renders a diff row with both quantities and the shared count", () => {
    render(<SetComparisonView {...baseProps()} />);

    const row = screen.getByText("Brick 2 x 4").closest("tr");
    expect(row).not.toBeNull();
    const cells = within(row as HTMLElement);
    expect(cells.getByText("Red")).toBeInTheDocument();
    expect(cells.getByText("5")).toBeInTheDocument();
    expect(cells.getByText("Both")).toBeInTheDocument();
  });

  it("calls onCategoryChange when a category filter is selected", async () => {
    const props = baseProps();
    render(<SetComparisonView {...props} />);

    await userEvent.click(screen.getByRole("button", { name: /only a/i }));

    expect(props.onCategoryChange).toHaveBeenCalledWith("ONLY_A");
  });

  it("shows a loading indicator", () => {
    render(<SetComparisonView {...baseProps()} isLoading report={undefined} />);
    expect(screen.getByLabelText(/loading/i)).toBeInTheDocument();
  });

  it("shows a not-imported message on 404", () => {
    render(
      <SetComparisonView
        {...baseProps()}
        isError
        error={new ApiError(404, "Not Found")}
        report={undefined}
      />,
    );
    expect(screen.getByText(/import/i)).toBeInTheDocument();
  });

  it("shows an empty state when the filter yields no lines", () => {
    render(
      <SetComparisonView
        {...baseProps()}
        report={{ ...report, lines: [], totalLines: 0 }}
      />,
    );
    expect(screen.getByText(/no parts to show/i)).toBeInTheDocument();
  });
});
