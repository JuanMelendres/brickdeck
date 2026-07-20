import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AddPriceSnapshotForm } from "./AddPriceSnapshotForm";

describe("AddPriceSnapshotForm", () => {
  it("submits a parsed snapshot payload", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<AddPriceSnapshotForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.clear(screen.getByLabelText(/amount/i));
    await user.type(screen.getByLabelText(/amount/i), "129.99");
    await user.clear(screen.getByLabelText(/currency/i));
    await user.type(screen.getByLabelText(/currency/i), "usd");
    await user.type(screen.getByLabelText(/observed/i), "2026-01-10");
    await user.click(screen.getByRole("button", { name: /add price/i }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        amount: 129.99,
        currency: "USD",
        condition: "NEW",
        observedAt: "2026-01-10",
      }),
    );
  });

  it("rejects a non-positive amount", async () => {
    const onSubmit = vi.fn();
    render(<AddPriceSnapshotForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.clear(screen.getByLabelText(/amount/i));
    await user.type(screen.getByLabelText(/amount/i), "0");
    await user.type(screen.getByLabelText(/observed/i), "2026-01-10");
    await user.click(screen.getByRole("button", { name: /add price/i }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/valid price/i)).toBeInTheDocument();
  });
});
