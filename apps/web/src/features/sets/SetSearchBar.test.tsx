import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SetSearchBar } from "./SetSearchBar";

describe("SetSearchBar", () => {
  it("calls onSearch with the trimmed query on submit", async () => {
    const onSearch = vi.fn();
    render(<SetSearchBar onSearch={onSearch} />);

    await userEvent.type(
      screen.getByRole("textbox", { name: /search sets/i }),
      "  x-wing  ",
    );
    await userEvent.click(screen.getByRole("button", { name: /search/i }));

    expect(onSearch).toHaveBeenCalledExactlyOnceWith("x-wing");
  });

  it("blocks submitting an empty query and shows a validation message", async () => {
    const onSearch = vi.fn();
    render(<SetSearchBar onSearch={onSearch} />);

    await userEvent.click(screen.getByRole("button", { name: /search/i }));

    expect(onSearch).not.toHaveBeenCalled();
    expect(await screen.findByText(/enter a search term/i)).toBeInTheDocument();
  });
});
