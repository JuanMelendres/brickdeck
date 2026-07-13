import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CollectionPage from "./page";
import type { PageResponse } from "@/lib/types/api";
import type {
  UserPartResponse,
  UserSetResponse,
} from "@/lib/types/collection";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  }),
}));

const { useCollectionSetsMock, useCollectionPartsMock } = vi.hoisted(() => ({
  useCollectionSetsMock: vi.fn(),
  useCollectionPartsMock: vi.fn(),
}));

const set: UserSetResponse = {
  id: "cs1",
  setNumber: "75257-1",
  setName: "Millennium Falcon",
  yearReleased: 2019,
  themeName: "Star Wars",
  imageUrl: null,
  status: "OWNED",
  purchasePrice: 159.99,
  purchaseDate: "2026-01-15",
};

function setsPage(overrides: Partial<PageResponse<UserSetResponse>> = {}) {
  return {
    content: [set],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  } satisfies PageResponse<UserSetResponse>;
}

vi.mock("@/features/collection/collectionSetsHooks", () => ({
  useCollectionSets: (page: number, size: number) => ({
    data: useCollectionSetsMock(page, size) ?? setsPage(),
    isLoading: false,
    isError: false,
  }),
  useAddCollectionSet: () => ({ mutateAsync: vi.fn() }),
  useUpdateCollectionSet: () => ({ mutateAsync: vi.fn() }),
  useRemoveCollectionSet: () => ({
    mutate: vi.fn(),
    isPending: false,
    variables: undefined,
  }),
}));

const part: UserPartResponse = {
  id: "cp1",
  partNumber: "3001",
  partName: "Brick 2 x 4",
  partImageUrl: null,
  colorExternalId: 4,
  colorName: "Red",
  colorRgb: "B40000",
  quantity: 10,
  storageLocation: "Bin A",
};

function partsPage(overrides: Partial<PageResponse<UserPartResponse>> = {}) {
  return {
    content: [part],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  } satisfies PageResponse<UserPartResponse>;
}

vi.mock("@/features/collection/collectionPartsHooks", () => ({
  useCollectionParts: (page: number, size: number) => ({
    data: useCollectionPartsMock(page, size) ?? partsPage(),
    isLoading: false,
    isError: false,
  }),
  useAddCollectionPart: () => ({ mutateAsync: vi.fn() }),
  useUpdateCollectionPart: () => ({ mutateAsync: vi.fn() }),
  useRemoveCollectionPart: () => ({
    mutate: vi.fn(),
    isPending: false,
    variables: undefined,
  }),
}));

describe("CollectionPage", () => {
  it("renders the heading, owned sets, and loose parts sections", () => {
    useCollectionSetsMock.mockReturnValue(setsPage());
    useCollectionPartsMock.mockReturnValue(partsPage());
    render(<CollectionPage />);
    expect(
      screen.getByRole("heading", { name: /my collection/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /owned sets/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /loose parts/i }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/set number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/part number/i)).toBeInTheDocument();
    expect(screen.getByText("Millennium Falcon")).toBeInTheDocument();
    expect(screen.getByText("Brick 2 x 4")).toBeInTheDocument();
  });

  it("advances the owned-sets page when Next is clicked", async () => {
    const user = userEvent.setup();
    // Sets span two pages; keep parts on a single page (no pager).
    useCollectionSetsMock.mockReturnValue(
      setsPage({ totalPages: 2, last: false }),
    );
    useCollectionPartsMock.mockReturnValue(partsPage());
    render(<CollectionPage />);

    // First render requested page 0.
    expect(useCollectionSetsMock).toHaveBeenCalledWith(0, 20);

    await user.click(screen.getByRole("button", { name: /next/i }));

    // Clicking Next re-invokes the sets hook with the next page.
    expect(useCollectionSetsMock).toHaveBeenCalledWith(1, 20);
  });
});
