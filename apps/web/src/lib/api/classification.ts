import { apiPostMultipart } from "./client";
import type { PartClassificationResponse } from "@/lib/types/classification";

/** Classify a single-part photo into ranked part/color candidates (ADR-013). */
export function classifyPart(
  image: File,
): Promise<PartClassificationResponse> {
  const formData = new FormData();
  formData.append("image", image);
  return apiPostMultipart<PartClassificationResponse>(
    "/api/v1/classify/part",
    formData,
  );
}
