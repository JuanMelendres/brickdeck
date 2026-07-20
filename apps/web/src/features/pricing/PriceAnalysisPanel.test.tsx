import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PriceAnalysisPanel } from "./PriceAnalysisPanel";
import { ApiError } from "@/lib/api/client";
import type { PriceAnalysisResponse } from "@/lib/types/pricing";

const analysis: PriceAnalysisResponse = {
  setNumber: "75257-1",
  currency: "USD",
  snapshotCount: 3,
  minAmount: 80,
  averageAmount: 100,
  maxAmount: 120,
  latestAmount: 120,
  numberOfParts: 100,
  pricePerPiece: 1,
  candidate: {
    amount: 80,
    pricePerPiece: 0.8,
    percentBelowAverage: 20,
    atOrBelowLowest: true,
    verdict: "GREAT_DEAL",
  },
};

function baseProps() {
  return {
    currency: "USD",
    candidatePrice: "80",
    onCurrencyChange: vi.fn(),
    onCandidatePriceChange: vi.fn(),
    isLoading: false,
    isError: false,
    error: undefined as unknown,
    data: analysis,
  };
}

describe("PriceAnalysisPanel", () => {
  it("shows aggregates and the deal verdict", () => {
    render(<PriceAnalysisPanel {...baseProps()} />);
    expect(screen.getByText(/great deal/i)).toBeInTheDocument();
    // Average shown somewhere.
    expect(screen.getByText(/100/)).toBeInTheDocument();
  });

  it("prompts to add a snapshot when none exist (404)", () => {
    render(
      <PriceAnalysisPanel
        {...baseProps()}
        isError
        error={new ApiError(404, "No price snapshots")}
        data={undefined}
      />,
    );
    expect(screen.getByText(/no price data/i)).toBeInTheDocument();
  });

  it("shows a loading indicator", () => {
    render(<PriceAnalysisPanel {...baseProps()} isLoading data={undefined} />);
    expect(screen.getByLabelText(/loading/i)).toBeInTheDocument();
  });

  it("reports candidate price changes", async () => {
    const props = baseProps();
    render(<PriceAnalysisPanel {...props} candidatePrice="" />);

    await userEvent.type(screen.getByLabelText(/candidate price/i), "9");

    expect(props.onCandidatePriceChange).toHaveBeenCalled();
  });
});
