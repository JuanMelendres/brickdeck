import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { TriggeredAlertsList } from "./TriggeredAlertsList";
import type { TriggeredAlertResponse } from "@/lib/types/alerts";

const alert: TriggeredAlertResponse = {
  id: "t1",
  ruleId: "r1",
  setNumber: "75257-1",
  amount: 80,
  currency: "USD",
  message: "80 is below your target 100",
  triggeredAt: "2026-01-11T00:00:00",
};

describe("TriggeredAlertsList", () => {
  it("shows an empty state when there are no alerts", () => {
    render(<TriggeredAlertsList alerts={[]} onDismiss={vi.fn()} />);
    expect(screen.getByText(/no triggered alerts/i)).toBeInTheDocument();
  });

  it("renders an alert and dismisses it", async () => {
    const onDismiss = vi.fn();
    render(<TriggeredAlertsList alerts={[alert]} onDismiss={onDismiss} />);

    expect(screen.getByText(/below your target/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /dismiss/i }));

    expect(onDismiss).toHaveBeenCalledWith("t1");
  });
});
