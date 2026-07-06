import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PartsInventory } from "./PartsInventory";
import { ApiError } from "@/lib/api/client";
import type { PageResponse, SetPartResponse } from "@/lib/types/api";

const part: SetPartResponse = {
  id: "1",
  setNumber: "75257-1",
  partNumber: "3001",
  partName: "Brick 2 x 4",
  partImageUrl: null,
  colorExternalId: 4,
  colorName: "Red",
  colorRgb: "C91A09",
  quantity: 6,
  spare: false,
  elementId: "300121",
};

const page = (content: SetPartResponse[]): PageResponse<SetPartResponse> => ({
  content,
  page: 0,
  size: 50,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
});

const baseProps = {
  isFetching: false,
  isError: false,
  error: null,
  data: undefined as PageResponse<SetPartResponse> | undefined,
  page: 0,
  onPageChange: vi.fn(),
  onImport: vi.fn(),
  isImporting: false,
  importError: null as unknown,
};

describe("PartsInventory", () => {
  it("shows a progress indicator while fetching", () => {
    render(<PartsInventory {...baseProps} isFetching />);
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("shows the backend error message on failure", () => {
    render(
      <PartsInventory
        {...baseProps}
        isError
        error={new ApiError(404, "Set not imported")}
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Set not imported");
  });

  it("offers an import button when inventory is empty and calls onImport", async () => {
    const onImport = vi.fn();
    render(<PartsInventory {...baseProps} data={page([])} onImport={onImport} />);

    expect(screen.getByText(/no inventory imported/i)).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: /import inventory/i }),
    );
    expect(onImport).toHaveBeenCalledOnce();
  });

  it("disables the import button while importing", () => {
    render(<PartsInventory {...baseProps} data={page([])} isImporting />);
    expect(
      screen.getByRole("button", { name: /importing/i }),
    ).toBeDisabled();
  });

  it("renders a row per part with color and quantity", () => {
    render(<PartsInventory {...baseProps} data={page([part])} />);
    expect(screen.getByText("Brick 2 x 4")).toBeInTheDocument();
    expect(screen.getByText("Red")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "6" })).toBeInTheDocument();
  });
});
