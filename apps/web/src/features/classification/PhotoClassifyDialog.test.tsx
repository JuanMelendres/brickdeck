import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PhotoClassifyDialog } from "./PhotoClassifyDialog";
import type { PartClassificationResponse } from "@/lib/types/classification";
import * as imageDownscale from "./imageDownscale";

function uploadFile(input: HTMLInputElement, file: File) {
  Object.defineProperty(input, "files", { value: [file], writable: false });
  fireEvent.change(input);
}

const response: PartClassificationResponse = {
  colorSuggestion: { colorId: 4, colorName: "Red", confidence: 0.95 },
  partSuggestions: [
    {
      partNumber: "3001",
      partNameGuess: "Brick 2 x 4",
      score: 0.84,
      resolutionStatus: "RESOLVED",
      referenceImageUrl: "https://cdn.rebrickable.com/media/parts/elements/300121.jpg",
    },
    {
      partNumber: "99999",
      partNameGuess: "Unknown Part",
      score: 0.4,
      resolutionStatus: "UNRESOLVED",
      referenceImageUrl: null,
    },
  ],
};

describe("PhotoClassifyDialog", () => {
  afterEach(() => vi.restoreAllMocks());

  it("classifies the uploaded photo and shows ranked suggestions with the top one preselected", async () => {
    vi.spyOn(imageDownscale, "downscaleImage").mockImplementation(
      async (file) => file,
    );
    const onClassify = vi.fn().mockResolvedValue(response);
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();

    render(
      <PhotoClassifyDialog
        onClassify={onClassify}
        onConfirm={onConfirm}
        onClose={onClose}
      />,
    );

    const input = screen.getByTestId("photo-file-input") as HTMLInputElement;
    const file = new File(["fake"], "photo.jpg", { type: "image/jpeg" });
    uploadFile(input, file);

    await waitFor(() => expect(onClassify).toHaveBeenCalledWith(file));
    await screen.findByText(/3001/);
    expect(screen.getByText(/Red/)).toBeInTheDocument();

    const radios = screen.getAllByRole("radio") as HTMLInputElement[];
    expect(radios[0].checked).toBe(true);
  });

  it("shows a classify error", async () => {
    vi.spyOn(imageDownscale, "downscaleImage").mockImplementation(
      async (file) => file,
    );
    const onClassify = vi.fn().mockRejectedValue(new Error("Bad photo"));

    render(
      <PhotoClassifyDialog
        onClassify={onClassify}
        onConfirm={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    const input = screen.getByTestId("photo-file-input") as HTMLInputElement;
    uploadFile(input, new File(["fake"], "photo.jpg", { type: "image/jpeg" }));

    await screen.findByText("Bad photo");
  });

  it("disables confirm and warns when the guessed color isn't in the local catalog", async () => {
    vi.spyOn(imageDownscale, "downscaleImage").mockImplementation(
      async (file) => file,
    );
    const onClassify = vi.fn().mockResolvedValue({
      colorSuggestion: { colorId: null, colorName: "Not-A-Real-Color", confidence: 0.5 },
      partSuggestions: [response.partSuggestions[0]],
    } satisfies PartClassificationResponse);

    render(
      <PhotoClassifyDialog
        onClassify={onClassify}
        onConfirm={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    const input = screen.getByTestId("photo-file-input") as HTMLInputElement;
    uploadFile(input, new File(["fake"], "photo.jpg", { type: "image/jpeg" }));

    await screen.findByText(/not in your catalog/i);
    expect(screen.getByRole("button", { name: /add to collection/i })).toBeDisabled();
  });

  it("confirms with the selected part, resolved color, quantity and storage", async () => {
    vi.spyOn(imageDownscale, "downscaleImage").mockImplementation(
      async (file) => file,
    );
    const onClassify = vi.fn().mockResolvedValue(response);
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();
    const user = userEvent.setup();

    render(
      <PhotoClassifyDialog
        onClassify={onClassify}
        onConfirm={onConfirm}
        onClose={onClose}
      />,
    );

    const input = screen.getByTestId("photo-file-input") as HTMLInputElement;
    uploadFile(input, new File(["fake"], "photo.jpg", { type: "image/jpeg" }));
    await screen.findByText(/3001/);

    const quantityField = screen.getByLabelText(/quantity/i);
    await user.clear(quantityField);
    await user.type(quantityField, "5");
    await user.type(screen.getByLabelText(/storage location/i), "Bin A3");

    await user.click(screen.getByRole("button", { name: /add to collection/i }));

    await waitFor(() =>
      expect(onConfirm).toHaveBeenCalledWith({
        externalPartNumber: "3001",
        colorExternalId: 4,
        quantity: 5,
        storageLocation: "Bin A3",
      }),
    );
    expect(onClose).toHaveBeenCalled();
  });
});
