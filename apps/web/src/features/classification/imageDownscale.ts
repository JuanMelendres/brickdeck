/**
 * Client-side downscale before upload: vision-API cost/latency scale with
 * image size, and a phone photo of one brick doesn't need full resolution
 * (spike Finding 5). Returns the original file unchanged when it's already
 * within `maxDimension`.
 */
export async function downscaleImage(
  file: File,
  maxDimension: number,
): Promise<File> {
  const bitmap = await createImageBitmap(file);
  try {
    if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) {
      return file;
    }

    const scale = maxDimension / Math.max(bitmap.width, bitmap.height);
    const targetWidth = Math.round(bitmap.width * scale);
    const targetHeight = Math.round(bitmap.height * scale);

    const canvas = document.createElement("canvas");
    canvas.width = targetWidth;
    canvas.height = targetHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) {
      return file;
    }
    ctx.drawImage(bitmap, 0, 0, targetWidth, targetHeight);

    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, "image/jpeg", 0.85);
    });
    if (!blob) {
      return file;
    }

    return new File([blob], file.name, { type: "image/jpeg" });
  } finally {
    bitmap.close();
  }
}
