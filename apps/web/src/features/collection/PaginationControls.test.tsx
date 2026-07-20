import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PaginationControls } from "./PaginationControls";

describe("PaginationControls", () => {
  it("renders nothing when there is a single page", () => {
    const { container } = render(
      <PaginationControls
        page={0}
        totalPages={1}
        first
        last
        onPageChange={vi.fn()}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the 1-indexed page position", () => {
    render(
      <PaginationControls
        page={1}
        totalPages={3}
        first={false}
        last={false}
        onPageChange={vi.fn()}
      />,
    );
    expect(screen.getByText(/page 2 of 3/i)).toBeInTheDocument();
  });

  it("disables Previous on the first page", () => {
    render(
      <PaginationControls
        page={0}
        totalPages={3}
        first
        last={false}
        onPageChange={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: /previous/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /next/i })).toBeEnabled();
  });

  it("disables Next on the last page", () => {
    render(
      <PaginationControls
        page={2}
        totalPages={3}
        first={false}
        last
        onPageChange={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: /next/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /previous/i })).toBeEnabled();
  });

  it("requests the neighbouring page on click", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    render(
      <PaginationControls
        page={1}
        totalPages={3}
        first={false}
        last={false}
        onPageChange={onPageChange}
      />,
    );
    await user.click(screen.getByRole("button", { name: /previous/i }));
    await user.click(screen.getByRole("button", { name: /next/i }));
    expect(onPageChange).toHaveBeenNthCalledWith(1, 0);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 2);
  });
});
