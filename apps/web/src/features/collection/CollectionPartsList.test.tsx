import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CollectionPartsList } from "./CollectionPartsList";
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

describe("CollectionPartsList", () => {
  it("shows an empty state when there are no parts", () => {
    render(<CollectionPartsList parts={[]} onRemove={vi.fn()} />);
    expect(screen.getByText(/no loose parts/i)).toBeInTheDocument();
  });

  it("renders a row per part", () => {
    render(<CollectionPartsList parts={[part]} onRemove={vi.fn()} />);
    expect(screen.getByText("Brick 2 x 4")).toBeInTheDocument();
    expect(screen.getByText("Red")).toBeInTheDocument();
    expect(screen.getByText("Bin A")).toBeInTheDocument();
  });

  it("calls onRemove with the part id", async () => {
    const onRemove = vi.fn();
    render(<CollectionPartsList parts={[part]} onRemove={onRemove} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /remove/i }));

    expect(onRemove).toHaveBeenCalledWith("cp1");
  });
});
