import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CollectionSetsList } from "./CollectionSetsList";
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

describe("CollectionSetsList", () => {
  it("shows an empty state when there are no sets", () => {
    render(<CollectionSetsList sets={[]} onRemove={vi.fn()} />);
    expect(screen.getByText(/no sets/i)).toBeInTheDocument();
  });

  it("renders a row per set", () => {
    render(<CollectionSetsList sets={[set]} onRemove={vi.fn()} />);
    expect(screen.getByText("Millennium Falcon")).toBeInTheDocument();
    expect(screen.getByText("75257-1")).toBeInTheDocument();
  });

  it("calls onRemove with the set id", async () => {
    const onRemove = vi.fn();
    render(<CollectionSetsList sets={[set]} onRemove={onRemove} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /remove/i }));

    expect(onRemove).toHaveBeenCalledWith("cs1");
  });
});
