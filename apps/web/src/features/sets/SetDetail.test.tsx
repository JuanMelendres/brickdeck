import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { SetDetail } from "./SetDetail";
import type { BrickSetResponse } from "@/lib/types/api";

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
  cacheStatus: "LOCAL_CACHE_HIT",
};

describe("SetDetail", () => {
  it("renders the set name as a heading", () => {
    render(<SetDetail set={set} />);
    expect(
      screen.getByRole("heading", { name: "Millennium Falcon" }),
    ).toBeInTheDocument();
  });

  it("shows number, year, theme and part count", () => {
    render(<SetDetail set={set} />);
    expect(screen.getByText(/75257-1/)).toBeInTheDocument();
    expect(screen.getByText(/2019/)).toBeInTheDocument();
    expect(screen.getByText(/Star Wars/)).toBeInTheDocument();
    expect(screen.getByText(/1351/)).toBeInTheDocument();
  });
});
