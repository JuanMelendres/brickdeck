import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { RequireAuth } from "./RequireAuth";
import * as useAuthModule from "./useAuth";
import type { AuthContextValue, AuthStatus } from "./AuthProvider";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
}));

function mockAuth(status: AuthStatus) {
  vi.spyOn(useAuthModule, "useAuth").mockReturnValue({
    status,
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  } as AuthContextValue);
}

describe("RequireAuth", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
  });

  it("renders children when authenticated", () => {
    mockAuth("authenticated");
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    expect(screen.getByText("secret")).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("shows a loader and no children while loading", () => {
    mockAuth("loading");
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("redirects to /login when unauthenticated", async () => {
    mockAuth("unauthenticated");
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });
});
