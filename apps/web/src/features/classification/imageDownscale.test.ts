import { afterEach, describe, expect, it, vi } from "vitest";
import { downscaleImage } from "./imageDownscale";

function mockBitmap(width: number, height: number) {
  return { width, height, close: vi.fn() };
}

describe("downscaleImage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("returns the original file unchanged when already within the max dimension", async () => {
    const bitmap = mockBitmap(400, 300);
    vi.stubGlobal("createImageBitmap", vi.fn().mockResolvedValue(bitmap));
    const getContextSpy = vi.spyOn(
      HTMLCanvasElement.prototype,
      "getContext",
    );
    const file = new File(["fake-bytes"], "photo.jpg", { type: "image/jpeg" });

    const result = await downscaleImage(file, 1024);

    expect(result).toBe(file);
    expect(getContextSpy).not.toHaveBeenCalled();
    expect(bitmap.close).toHaveBeenCalled();
  });

  it("draws to a scaled-down canvas and returns a new File when the image exceeds the max dimension", async () => {
    const bitmap = mockBitmap(3000, 2000);
    vi.stubGlobal("createImageBitmap", vi.fn().mockResolvedValue(bitmap));
    const drawImage = vi.fn();
    vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue({
      drawImage,
    } as unknown as CanvasRenderingContext2D);
    const resizedBlob = new Blob(["resized-bytes"], { type: "image/jpeg" });
    vi.spyOn(HTMLCanvasElement.prototype, "toBlob").mockImplementation(
      (callback) => callback(resizedBlob),
    );
    const file = new File(["fake-bytes"], "photo.jpg", { type: "image/jpeg" });

    const result = await downscaleImage(file, 1024);

    expect(result).not.toBe(file);
    expect(result).toBeInstanceOf(File);
    expect(result.name).toBe("photo.jpg");
    expect(result.type).toBe("image/jpeg");
    expect(drawImage).toHaveBeenCalledWith(bitmap, 0, 0, 1024, 683);
    expect(bitmap.close).toHaveBeenCalled();
  });
});
