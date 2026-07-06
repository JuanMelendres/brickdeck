import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { SetResults } from "./SetResults";
import { ApiError } from "@/lib/api/client";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";

const set: BrickSetResponse = {
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

const pageWith = (content: BrickSetResponse[]): PageResponse<BrickSetResponse> => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
});

describe("SetResults", () => {
  it("prompts to search when no query has been made", () => {
    render(<SetResults hasQuery={false} isFetching={false} isError={false} error={null} data={undefined} />);
    expect(screen.getByText(/search for a lego set/i)).toBeInTheDocument();
  });

  it("shows a progress indicator while fetching", () => {
    render(<SetResults hasQuery isFetching isError={false} error={null} data={undefined} />);
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("shows the backend error message on failure", () => {
    render(
      <SetResults
        hasQuery
        isFetching={false}
        isError
        error={new ApiError(502, "Rebrickable unavailable")}
        data={undefined}
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Rebrickable unavailable");
  });

  it("shows an empty state when the search returns no sets", () => {
    render(<SetResults hasQuery isFetching={false} isError={false} error={null} data={pageWith([])} />);
    expect(screen.getByText(/no sets found/i)).toBeInTheDocument();
  });

  it("renders a card per set when there are results", () => {
    render(<SetResults hasQuery isFetching={false} isError={false} error={null} data={pageWith([set])} />);
    expect(screen.getByText("Millennium Falcon")).toBeInTheDocument();
    expect(screen.getByText(/75257-1/)).toBeInTheDocument();
  });
});
