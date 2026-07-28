import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { classifyPart } from "./classification";
import type { PartClassificationResponse } from "@/lib/types/classification";

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
  ],
};

describe("classifyPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs the image as multipart form data and returns the parsed response", async () => {
    const spy = vi
      .spyOn(client, "apiPostMultipart")
      .mockResolvedValue(response);
    const file = new File(["fake-bytes"], "photo.jpg", { type: "image/jpeg" });

    const result = await classifyPart(file);

    expect(result).toBe(response);
    expect(spy).toHaveBeenCalledTimes(1);
    const [path, formData] = spy.mock.calls[0];
    expect(path).toBe("/api/v1/classify/part");
    expect(formData).toBeInstanceOf(FormData);
    expect((formData as FormData).get("image")).toBe(file);
  });
});
