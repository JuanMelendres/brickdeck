import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MissingPartsPanel } from "./MissingPartsPanel";
import { ApiError } from "@/lib/api/client";
import type { MissingPartsReport } from "@/lib/types/missingParts";

const report: MissingPartsReport = {
  setNumber: "75257-1",
  totalRequired: 6,
  totalOwned: 4,
  totalMissing: 2,
  completionPercentage: 66.7,
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
};

describe("MissingPartsPanel", () => {
  it("prompts unauthenticated users to log in", () => {
    render(
      <MissingPartsPanel
        isAuthenticated={false}
        isLoading={false}
        isError={false}
        error={null}
        report={undefined}
      />,
    );
    expect(screen.getByRole("link", { name: /log in/i })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it("shows a loader while fetching", () => {
    render(
      <MissingPartsPanel
        isAuthenticated
        isLoading
        isError={false}
        error={null}
        report={undefined}
      />,
    );
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("tells the user to import inventory on a 404", () => {
    render(
      <MissingPartsPanel
        isAuthenticated
        isLoading={false}
        isError
        error={new ApiError(404, "Set inventory not imported")}
        report={undefined}
      />,
    );
    expect(screen.getByText(/import/i)).toBeInTheDocument();
  });

  it("renders completion and a per-part row from the report", () => {
    render(
      <MissingPartsPanel
        isAuthenticated
        isLoading={false}
        isError={false}
        error={null}
        report={report}
      />,
    );
    expect(screen.getByText(/66.7%/)).toBeInTheDocument();
    expect(screen.getByText("Brick 2 x 4")).toBeInTheDocument();
    expect(screen.getByText(/2 missing/i)).toBeInTheDocument();
  });
});
