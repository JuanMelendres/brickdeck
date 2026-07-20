import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { PriceTrackingSection } from "./PriceTrackingSection";
import type { PriceAnalysisResponse } from "@/lib/types/pricing";

const { usePriceAnalysisMock, useAddPriceSnapshotMock } = vi.hoisted(() => ({
  usePriceAnalysisMock: vi.fn(),
  useAddPriceSnapshotMock: vi.fn(),
}));

vi.mock("./pricingHooks", () => ({
  usePriceAnalysis: usePriceAnalysisMock,
  useAddPriceSnapshot: useAddPriceSnapshotMock,
}));

const analysis = {
  setNumber: "75257-1",
  currency: "USD",
  snapshotCount: 1,
  minAmount: 80,
  averageAmount: 80,
  maxAmount: 80,
  latestAmount: 80,
  numberOfParts: 100,
  pricePerPiece: 0.8,
  candidate: null,
} as PriceAnalysisResponse;

describe("PriceTrackingSection", () => {
  afterEach(() => vi.clearAllMocks());

  function setup(isAuthenticated: boolean) {
    usePriceAnalysisMock.mockReturnValue({
      data: analysis,
      isLoading: false,
      isError: false,
      error: null,
    });
    useAddPriceSnapshotMock.mockReturnValue({ mutateAsync: vi.fn() });
    render(
      <PriceTrackingSection setNumber="75257-1" isAuthenticated={isAuthenticated} />,
    );
  }

  it("prompts to log in when unauthenticated", () => {
    setup(false);
    expect(screen.getByText(/log in/i)).toBeInTheDocument();
    expect(usePriceAnalysisMock).toHaveBeenCalledWith(
      "75257-1",
      expect.anything(),
      false,
    );
  });

  it("shows the add form and analysis when authenticated", () => {
    setup(true);
    expect(screen.getByRole("button", { name: /add price/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/candidate price/i)).toBeInTheDocument();
  });
});
