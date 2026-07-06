/** Centralized TanStack Query keys. Invalidate by prefix. */
export const queryKeys = {
  sets: {
    all: ["sets"] as const,
    search: (query: string, page: number, size: number) =>
      ["sets", "search", query, page, size] as const,
  },
};
