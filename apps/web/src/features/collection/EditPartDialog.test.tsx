import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EditPartDialog } from "./EditPartDialog";
import { ApiError } from "@/lib/api/client";
import type { UserPartResponse } from "@/lib/types/collection";

const part: UserPartResponse = {
  id: "cp1",
  partNumber: "3001",
  partName: "Brick 2 x 4",
  partImageUrl: null,
  colorExternalId: 4,
  colorName: "Red",
  colorRgb: "B40000",
  quantity: 10,
  storageLocation: "Bin A",
};

describe("EditPartDialog", () => {
  it("prefills the current values", () => {
    render(<EditPartDialog part={part} onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText(/quantity/i)).toHaveValue(10);
    expect(screen.getByLabelText(/storage/i)).toHaveValue("Bin A");
    expect(screen.getByText(/brick 2 x 4/i)).toBeInTheDocument();
  });

  it("submits the changed fields", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<EditPartDialog part={part} onClose={vi.fn()} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    const qty = screen.getByLabelText(/quantity/i);
    await user.clear(qty);
    await user.type(qty, "25");
    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith("cp1", {
        quantity: 25,
        storageLocation: "Bin A",
      }),
    );
  });

  it("omits storage cleared to empty", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<EditPartDialog part={part} onClose={vi.fn()} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.clear(screen.getByLabelText(/storage/i));
    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith("cp1", { quantity: 10 }),
    );
  });

  it("validates quantity of at least 1", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<EditPartDialog part={part} onClose={vi.fn()} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    const qty = screen.getByLabelText(/quantity/i);
    await user.clear(qty);
    await user.type(qty, "0");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/at least 1/i)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("surfaces a server error and keeps the dialog open", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError(400, "Invalid quantity"));
    const onClose = vi.fn();
    render(
      <EditPartDialog part={part} onClose={onClose} onSubmit={onSubmit} />,
    );
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/invalid quantity/i)).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("closes on successful submit", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();
    render(
      <EditPartDialog part={part} onClose={onClose} onSubmit={onSubmit} />,
    );
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });
});
