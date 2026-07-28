import { useMutation } from "@tanstack/react-query";
import { classifyPart } from "@/lib/api/classification";

export function useClassifyPart() {
  return useMutation({
    mutationFn: (image: File) => classifyPart(image),
  });
}
