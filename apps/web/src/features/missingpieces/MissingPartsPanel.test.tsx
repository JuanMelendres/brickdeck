import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MissingPartsPanel } from "./MissingPartsPanel";
import { ApiError } from "@/lib/api/client";
import type { MissingPartsReport } from "@/lib/types/missingParts";

const baseReport: MissingPartsReport = {
  setNumber: "75257-1",
  totalRequired: 6,
  totalOwned: 4,
  totalMissing: 2,
  completionPercentage: 66.7,
  missingLineCount: 1,
  lines: [
    {
      partNumber: "3001",
      partName: "Brick 2 x 4",
      partImageUrl: null,
      colorExternalId: 4,
      colorName: "Red",
      colorRgb: "B40000",
      required: 4,
      owned: 2,
      missing: 2,
    },
  ],
  page: 0,
  size: 50,
  totalLines: 1,
  totalPages: 1,
  first: true,
  last: true,
};

const noop = () => {};

function renderPanel(props: Partial<Parameters<typeof MissingPartsPanel>[0]>) {
  return render(
    <MissingPartsPanel
      isAuthenticated
      isLoading={false}
      isError={false}
      error={null}
      report={undefined}
      missingOnly={false}
      onMissingOnlyChange={noop}
      onPageChange={noop}
      {...props}
    />,
  );
}

describe("MissingPartsPanel", () => {
  it("prompts unauthenticated users to log in", () => {
    renderPanel({ isAuthenticated: false });
    expect(screen.getByRole("link", { name: /log in/i })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it("shows a loader while fetching", () => {
    renderPanel({ isLoading: true });
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("tells the user to import inventory on a 404", () => {
    renderPanel({
      isError: true,
      error: new ApiError(404, "Set inventory not imported"),
    });
    expect(screen.getByText(/import/i)).toBeInTheDocument();
  });

  it("renders completion and a per-part row from the report", () => {
    renderPanel({ report: baseReport });
    expect(screen.getByText(/66.7%/)).toBeInTheDocument();
    expect(screen.getByText("Brick 2 x 4")).toBeInTheDocument();
    expect(screen.getByText(/2 missing/i)).toBeInTheDocument();
  });

  it("toggles the missing-only filter", async () => {
    const onMissingOnlyChange = vi.fn();
    renderPanel({ report: baseReport, onMissingOnlyChange });
    const user = userEvent.setup();

    await user.click(screen.getByRole("switch", { name: /only missing/i }));

    expect(onMissingOnlyChange).toHaveBeenCalledWith(true);
  });

  it("paginates when there is more than one page", async () => {
    const onPageChange = vi.fn();
    renderPanel({
      report: { ...baseReport, totalPages: 3, page: 1, first: false, last: false },
      onPageChange,
    });
    const user = userEvent.setup();

    expect(screen.getByText(/page 2 of 3/i)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /next/i }));
    expect(onPageChange).toHaveBeenCalledWith(2);
    await user.click(screen.getByRole("button", { name: /previous/i }));
    expect(onPageChange).toHaveBeenCalledWith(0);
  });
});
