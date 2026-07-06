/** Centralized TanStack Query keys. Invalidate by prefix. */
export const queryKeys = {
  sets: {
    all: ["sets"] as const,
    search: (query: string, page: number, size: number) =>
      ["sets", "search", query, page, size] as const,
    detail: (setNumber: string) => ["sets", "detail", setNumber] as const,
    partsAll: (setNumber: string) => ["sets", "parts", setNumber] as const,
    parts: (setNumber: string, page: number, size: number) =>
      ["sets", "parts", setNumber, page, size] as const,
  },
};
