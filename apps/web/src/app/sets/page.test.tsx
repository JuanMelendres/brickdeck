import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import SetsPage from "./page";
import * as setsApi from "@/lib/api/sets";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";

const page = (content: BrickSetResponse[]): PageResponse<BrickSetResponse> => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
});

const falcon: BrickSetResponse = {
  id: "1",
  externalSetNumber: "75257-1",
  name: "Millennium Falcon",
  yearReleased: 2019,
  themeId: null,
  themeName: "Star Wars",
  externalThemeId: 158,
  numberOfParts: 1351,
  imageUrl: null,
  externalUrl: "https://rebrickable.com/sets/75257-1/",
  source: "REBRICKABLE",
  cacheStatus: "EXTERNAL_SEARCH_RESULT",
};

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("SetsPage", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("searches and renders matching sets", async () => {
    const spy = vi
      .spyOn(setsApi, "searchSets")
      .mockResolvedValue(page([falcon]));

    render(<SetsPage />, { wrapper });

    await userEvent.type(
      screen.getByRole("textbox", { name: /search sets/i }),
      "falcon",
    );
    await userEvent.click(screen.getByRole("button", { name: /search/i }));

    expect(await screen.findByText("Millennium Falcon")).toBeInTheDocument();
    expect(spy).toHaveBeenCalledWith("falcon", 0, 20);
  });
});
