import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NavBar } from "./NavBar";
import * as useAuthModule from "@/features/auth/useAuth";
import type { AuthContextValue, AuthStatus } from "@/features/auth/AuthProvider";
import type { UserResponse } from "@/lib/types/auth";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

const logout = vi.fn();

function mockAuth(status: AuthStatus, user: UserResponse | null = null) {
  vi.spyOn(useAuthModule, "useAuth").mockReturnValue({
    status,
    user,
    login: vi.fn(),
    register: vi.fn(),
    logout,
  } as AuthContextValue);
}

const user: UserResponse = {
  id: "u1",
  email: "me@example.com",
  displayName: "Me",
  role: "USER",
  createdAt: "2026-01-01T00:00:00",
};

describe("NavBar", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    push.mockReset();
    logout.mockReset();
  });

  it("always shows the brand and a Sets link", () => {
    mockAuth("loading");
    render(<NavBar />);
    expect(screen.getByRole("link", { name: /brickdeck/i })).toHaveAttribute(
      "href",
      "/",
    );
    expect(screen.getByRole("link", { name: /sets/i })).toHaveAttribute(
      "href",
      "/sets",
    );
  });

  it("shows login and sign up links when unauthenticated", () => {
    mockAuth("unauthenticated");
    render(<NavBar />);
    expect(screen.getByRole("link", { name: /log in/i })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.getByRole("link", { name: /sign up/i })).toHaveAttribute(
      "href",
      "/register",
    );
    expect(
      screen.queryByRole("button", { name: /log out/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /collection/i }),
    ).not.toBeInTheDocument();
  });

  it("shows the user and a logout button when authenticated", () => {
    mockAuth("authenticated", user);
    render(<NavBar />);
    expect(screen.getByText("Me")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /log out/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /collection/i }),
    ).toHaveAttribute("href", "/collection");
    expect(
      screen.queryByRole("link", { name: /log in/i }),
    ).not.toBeInTheDocument();
  });

  it("logs out and returns home on click", async () => {
    mockAuth("authenticated", user);
    render(<NavBar />);
    const u = userEvent.setup();

    await u.click(screen.getByRole("button", { name: /log out/i }));

    expect(logout).toHaveBeenCalledTimes(1);
    expect(push).toHaveBeenCalledWith("/");
  });

  it("falls back to email when no display name", () => {
    mockAuth("authenticated", { ...user, displayName: null });
    render(<NavBar />);
    expect(screen.getByText("me@example.com")).toBeInTheDocument();
  });
});
