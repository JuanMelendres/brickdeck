import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AddSetToCollectionForm } from "./AddSetToCollectionForm";
import { ApiError } from "@/lib/api/client";

describe("AddSetToCollectionForm", () => {
  it("renders a set number field and an add button", () => {
    render(<AddSetToCollectionForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/set number/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /add set/i })).toBeInTheDocument();
  });

  it("requires a set number", async () => {
    const onSubmit = vi.fn();
    render(<AddSetToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /add set/i }));

    expect(await screen.findByText(/set number is required/i)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("submits the set number with the default OWNED status", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<AddSetToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set number/i), "75257-1");
    await user.click(screen.getByRole("button", { name: /add set/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        setNumber: "75257-1",
        status: "OWNED",
      }),
    );
  });

  it("shows a form-level alert when the server rejects a duplicate", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError(409, "Set already in collection"));
    render(<AddSetToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set number/i), "75257-1");
    await user.click(screen.getByRole("button", { name: /add set/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /already in collection/i,
    );
  });
});
