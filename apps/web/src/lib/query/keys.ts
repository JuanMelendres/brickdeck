import type { ComparisonCategory } from "@/lib/types/comparison";

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
    missingParts: (setNumber: string, missingOnly: boolean, page: number) =>
      ["sets", "missing-parts", setNumber, missingOnly, page] as const,
    comparison: (
      a: string,
      b: string,
      category: ComparisonCategory | null,
      page: number,
    ) => ["sets", "compare", a, b, category, page] as const,
  },
  alerts: {
    all: ["alerts"] as const,
    rules: (page: number) => ["alerts", "rules", page] as const,
    triggered: (page: number) => ["alerts", "triggered", page] as const,
  },
  pricing: {
    all: ["pricing"] as const,
    analysis: (
      setNumber: string,
      currency: string,
      candidatePrice: number | null,
    ) => ["pricing", "analysis", setNumber, currency, candidatePrice] as const,
    snapshots: (setNumber: string, page: number) =>
      ["pricing", "snapshots", setNumber, page] as const,
  },
  recommendations: {
    all: ["recommendations"] as const,
    buildable: (buildableOnly: boolean, page: number) =>
      ["recommendations", "buildable", buildableOnly, page] as const,
  },
  collection: {
    all: ["collection"] as const,
    setsAll: ["collection", "sets"] as const,
    sets: (page: number, size: number) =>
      ["collection", "sets", page, size] as const,
    partsAll: ["collection", "parts"] as const,
    parts: (page: number, size: number) =>
      ["collection", "parts", page, size] as const,
  },
};
