import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AddPartToCollectionForm } from "./AddPartToCollectionForm";
import { ApiError } from "@/lib/api/client";

describe("AddPartToCollectionForm", () => {
  it("renders part number, color, quantity fields and an add button", () => {
    render(<AddPartToCollectionForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/part number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/color id/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/quantity/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add part/i }),
    ).toBeInTheDocument();
  });

  it("requires a part number", async () => {
    const onSubmit = vi.fn();
    render(<AddPartToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/color id/i), "4");
    await user.click(screen.getByRole("button", { name: /add part/i }));

    expect(
      await screen.findByText(/part number is required/i),
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("submits the part with numeric color id and quantity", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<AddPartToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/part number/i), "3001");
    await user.type(screen.getByLabelText(/color id/i), "4");
    await user.clear(screen.getByLabelText(/quantity/i));
    await user.type(screen.getByLabelText(/quantity/i), "10");
    await user.click(screen.getByRole("button", { name: /add part/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        externalPartNumber: "3001",
        colorExternalId: 4,
        quantity: 10,
      }),
    );
  });

  it("shows an alert when the part is not in the catalog", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError(404, "Part or color not found in catalog"));
    render(<AddPartToCollectionForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/part number/i), "9999");
    await user.type(screen.getByLabelText(/color id/i), "4");
    await user.click(screen.getByRole("button", { name: /add part/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /not found in catalog/i,
    );
  });
});
