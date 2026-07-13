import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EditSetDialog } from "./EditSetDialog";
import { ApiError } from "@/lib/api/client";
import type { UserSetResponse } from "@/lib/types/collection";

const set: UserSetResponse = {
  id: "cs1",
  setNumber: "75257-1",
  setName: "Millennium Falcon",
  yearReleased: 2019,
  themeName: "Star Wars",
  imageUrl: null,
  status: "OWNED",
  purchasePrice: 159.99,
  purchaseDate: "2026-01-15",
};

describe("EditSetDialog", () => {
  it("prefills the current values", () => {
    render(<EditSetDialog set={set} onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText(/price/i)).toHaveValue(159.99);
    expect(screen.getByLabelText(/purchase date/i)).toHaveValue("2026-01-15");
    expect(screen.getByText(/millennium falcon/i)).toBeInTheDocument();
  });

  it("submits the changed fields", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<EditSetDialog set={set} onClose={vi.fn()} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    const price = screen.getByLabelText(/price/i);
    await user.clear(price);
    await user.type(price, "200");
    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith("cs1", {
        status: "OWNED",
        purchasePrice: 200,
        purchaseDate: "2026-01-15",
      }),
    );
  });

  it("omits fields cleared to empty", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<EditSetDialog set={set} onClose={vi.fn()} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.clear(screen.getByLabelText(/price/i));
    await user.clear(screen.getByLabelText(/purchase date/i));
    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith("cs1", { status: "OWNED" }),
    );
  });

  it("surfaces a server error and keeps the dialog open", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError(400, "Invalid status"));
    const onClose = vi.fn();
    render(<EditSetDialog set={set} onClose={onClose} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/invalid status/i)).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("closes on successful submit", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();
    render(<EditSetDialog set={set} onClose={onClose} onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });
});
