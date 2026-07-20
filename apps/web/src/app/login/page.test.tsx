import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginPage from "./page";
import * as useAuthModule from "@/features/auth/useAuth";
import type { AuthContextValue } from "@/features/auth/AuthProvider";

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace }),
}));

const login = vi.fn().mockResolvedValue(undefined);

function mockAuth(overrides: Partial<AuthContextValue> = {}) {
  vi.spyOn(useAuthModule, "useAuth").mockReturnValue({
    status: "unauthenticated",
    user: null,
    login,
    register: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  } as AuthContextValue);
}

describe("LoginPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    push.mockReset();
    replace.mockReset();
    login.mockClear();
  });

  it("renders the heading, form, and a link to register", () => {
    mockAuth();
    render(<LoginPage />);
    expect(
      screen.getByRole("heading", { name: /log in/i }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /create one/i }),
    ).toHaveAttribute("href", "/register");
  });

  it("logs in and redirects home on success", async () => {
    mockAuth();
    render(<LoginPage />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/email/i), "me@example.com");
    await user.type(screen.getByLabelText(/password/i), "password123");
    await user.click(screen.getByRole("button", { name: /log in/i }));

    await waitFor(() =>
      expect(login).toHaveBeenCalledWith({
        email: "me@example.com",
        password: "password123",
      }),
    );
    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
  });

  it("redirects home if already authenticated", async () => {
    mockAuth({ status: "authenticated" });
    render(<LoginPage />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });
});
