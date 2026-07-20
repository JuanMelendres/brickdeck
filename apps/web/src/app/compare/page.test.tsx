import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ComparePage from "./page";
import * as hook from "@/features/comparison/useSetComparison";
import type { SetComparisonReport } from "@/lib/types/comparison";

const replace = vi.fn();
let searchParams = new URLSearchParams();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  useSearchParams: () => searchParams,
  usePathname: () => "/compare",
}));

const report = {
  setNumberA: "75257-1",
  setNumberB: "10300-1",
  similarityScore: 0.42,
  sharedLineCount: 0,
  onlyALineCount: 0,
  onlyBLineCount: 0,
  lines: [],
  page: 0,
  size: 50,
  totalLines: 0,
  totalPages: 0,
  first: true,
  last: true,
} as SetComparisonReport;

function mockHook(overrides: Partial<ReturnType<typeof hook.useSetComparison>> = {}) {
  return vi.spyOn(hook, "useSetComparison").mockReturnValue({
    data: report,
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as ReturnType<typeof hook.useSetComparison>);
}

describe("ComparePage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    searchParams = new URLSearchParams();
  });

  it("prefills both set-number inputs from the URL and compares them", () => {
    searchParams = new URLSearchParams({ a: "75257-1", b: "10300-1" });
    const spy = mockHook();

    render(<ComparePage />);

    expect(screen.getByLabelText(/set a/i)).toHaveValue("75257-1");
    expect(screen.getByLabelText(/set b/i)).toHaveValue("10300-1");
    expect(spy).toHaveBeenCalledWith("75257-1", "10300-1", {
      category: undefined,
      page: 0,
    });
    expect(screen.getByText(/42% similar/i)).toBeInTheDocument();
  });

  it("pushes typed set numbers to the URL on submit", async () => {
    mockHook();
    render(<ComparePage />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/set a/i), "111-1");
    await user.type(screen.getByLabelText(/set b/i), "222-1");
    await user.click(screen.getByRole("button", { name: /compare/i }));

    expect(replace).toHaveBeenCalledWith("/compare?a=111-1&b=222-1");
  });
});
