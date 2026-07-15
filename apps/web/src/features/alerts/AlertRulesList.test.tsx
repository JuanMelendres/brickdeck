import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AlertRulesList } from "./AlertRulesList";
import type { PriceAlertRuleResponse } from "@/lib/types/alerts";

const rule: PriceAlertRuleResponse = {
  id: "r1",
  setNumber: "75257-1",
  currency: "USD",
  type: "BELOW_TARGET_PRICE",
  thresholdValue: 100,
  active: true,
  createdAt: "2026-01-10T00:00:00",
};

describe("AlertRulesList", () => {
  it("shows an empty state when there are no rules", () => {
    render(<AlertRulesList rules={[]} onDelete={vi.fn()} />);
    expect(screen.getByText(/no alert rules/i)).toBeInTheDocument();
  });

  it("renders a rule row and deletes it", async () => {
    const onDelete = vi.fn();
    render(<AlertRulesList rules={[rule]} onDelete={onDelete} />);

    expect(screen.getByText("75257-1")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /delete/i }));

    expect(onDelete).toHaveBeenCalledWith("r1");
  });
});
