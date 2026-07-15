import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CreateAlertRuleForm } from "./CreateAlertRuleForm";

describe("CreateAlertRuleForm", () => {
  it("submits a target-price rule with a parsed threshold", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<CreateAlertRuleForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set number/i), "75257-1");
    await user.clear(screen.getByLabelText(/currency/i));
    await user.type(screen.getByLabelText(/currency/i), "usd");
    await user.type(screen.getByLabelText(/threshold/i), "100");
    await user.click(screen.getByRole("button", { name: /create alert/i }));

    expect(onSubmit).toHaveBeenCalledWith({
      setNumber: "75257-1",
      currency: "USD",
      type: "BELOW_TARGET_PRICE",
      thresholdValue: 100,
    });
  });

  it("requires a threshold for a target-price rule", async () => {
    const onSubmit = vi.fn();
    render(<CreateAlertRuleForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set number/i), "75257-1");
    await user.click(screen.getByRole("button", { name: /create alert/i }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/threshold is required/i)).toBeInTheDocument();
  });

  it("omits the threshold for an at-or-below-lowest rule", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<CreateAlertRuleForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set number/i), "75257-1");
    await user.click(screen.getByLabelText(/alert type/i));
    await user.click(await screen.findByRole("option", { name: /at or below/i }));
    await user.click(screen.getByRole("button", { name: /create alert/i }));

    expect(onSubmit).toHaveBeenCalledWith({
      setNumber: "75257-1",
      currency: "USD",
      type: "AT_OR_BELOW_LOWEST",
    });
  });
});
