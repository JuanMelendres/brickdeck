import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { SetCard } from "./SetCard";
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
  cacheStatus: "EXTERNAL_SEARCH_RESULT",
};

describe("SetCard", () => {
  it("links the set name to its detail page", () => {
    render(<SetCard set={set} />);
    const link = screen.getByRole("link", { name: "Millennium Falcon" });
    expect(link).toHaveAttribute("href", "/sets/75257-1");
  });
});
