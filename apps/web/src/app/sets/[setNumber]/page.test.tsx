import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import SetDetailPage from "./page";
import * as setsApi from "@/lib/api/sets";
import type {
  BrickSetResponse,
  PageResponse,
  SetPartResponse,
} from "@/lib/types/api";

vi.mock("next/navigation", () => ({
  useParams: () => ({ setNumber: "75257-1" }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({
    status: "unauthenticated",
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  }),
}));

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

const part: SetPartResponse = {
  id: "1",
  setNumber: "75257-1",
  partNumber: "3001",
  partName: "Brick 2 x 4",
  partImageUrl: null,
  colorExternalId: 4,
  colorName: "Red",
  colorRgb: "C91A09",
  quantity: 6,
  spare: false,
  elementId: "300121",
};

const partsPage: PageResponse<SetPartResponse> = {
  content: [part],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("SetDetailPage", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("renders the set detail and its parts inventory", async () => {
    vi.spyOn(setsApi, "getSetByNumber").mockResolvedValue(set);
    vi.spyOn(setsApi, "getSetParts").mockResolvedValue(partsPage);

    render(<SetDetailPage />, { wrapper });

    expect(
      await screen.findByRole("heading", { name: "Millennium Falcon" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Brick 2 x 4")).toBeInTheDocument();
    // Missing-pieces section prompts unauthenticated users to log in.
    expect(
      await screen.findByRole("link", { name: /log in/i }),
    ).toHaveAttribute("href", "/login");
  });
});
