import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RegisterPage from "./page";
import * as useAuthModule from "@/features/auth/useAuth";
import type { AuthContextValue } from "@/features/auth/AuthProvider";

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace }),
}));

const registerFn = vi.fn().mockResolvedValue(undefined);

function mockAuth(overrides: Partial<AuthContextValue> = {}) {
  vi.spyOn(useAuthModule, "useAuth").mockReturnValue({
    status: "unauthenticated",
    user: null,
    login: vi.fn(),
    register: registerFn,
    logout: vi.fn(),
    ...overrides,
  } as AuthContextValue);
}

describe("RegisterPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    push.mockReset();
    replace.mockReset();
    registerFn.mockClear();
  });

  it("renders the heading, form, and a link to login", () => {
    mockAuth();
    render(<RegisterPage />);
    expect(
      screen.getByRole("heading", { name: /create account/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /log in/i }),
    ).toHaveAttribute("href", "/login");
  });

  it("registers and redirects home on success", async () => {
    mockAuth();
    render(<RegisterPage />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/email/i), "me@example.com");
    await user.type(screen.getByLabelText(/password/i), "password123");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    await waitFor(() =>
      expect(registerFn).toHaveBeenCalledWith({
        email: "me@example.com",
        password: "password123",
      }),
    );
    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
  });
});
