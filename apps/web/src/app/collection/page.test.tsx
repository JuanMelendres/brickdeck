import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
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
const page: PageResponse<UserSetResponse> = {
  content: [set],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

vi.mock("@/features/collection/collectionSetsHooks", () => ({
  useCollectionSets: () => ({ data: page, isLoading: false, isError: false }),
  useAddCollectionSet: () => ({ mutateAsync: vi.fn() }),
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
const partsPage: PageResponse<UserPartResponse> = {
  content: [part],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

vi.mock("@/features/collection/collectionPartsHooks", () => ({
  useCollectionParts: () => ({
    data: partsPage,
    isLoading: false,
    isError: false,
  }),
  useAddCollectionPart: () => ({ mutateAsync: vi.fn() }),
  useRemoveCollectionPart: () => ({
    mutate: vi.fn(),
    isPending: false,
    variables: undefined,
  }),
}));

describe("CollectionPage", () => {
  it("renders the heading, owned sets, and loose parts sections", () => {
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
});
