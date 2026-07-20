import { afterEach, describe, expect, it, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { AuthProvider } from "./AuthProvider";
import { useAuth } from "./useAuth";
import * as authApi from "@/lib/api/auth";
import * as tokenStore from "@/lib/auth/tokenStore";
import type { AuthResponse, UserResponse } from "@/lib/types/auth";

vi.mock("@/lib/api/auth");
vi.mock("@/lib/auth/tokenStore");

const user: UserResponse = {
  id: "u1",
  email: "me@example.com",
  displayName: "Me",
  role: "USER",
  createdAt: "2026-01-01T00:00:00",
};
const authResponse: AuthResponse = { token: "jwt-abc", tokenType: "Bearer", user };

const wrapper = ({ children }: { children: ReactNode }) => (
  <AuthProvider>{children}</AuthProvider>
);

describe("AuthProvider", () => {
  beforeEach(() => vi.resetAllMocks());
  afterEach(() => vi.restoreAllMocks());

  it("is unauthenticated when no token is stored", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue(null);

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.status).toBe("unauthenticated"));
    expect(result.current.user).toBeNull();
    expect(authApi.getMe).not.toHaveBeenCalled();
  });

  it("hydrates the user from getMe when a token exists", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue("jwt-abc");
    vi.mocked(authApi.getMe).mockResolvedValue(user);

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.status).toBe("authenticated"));
    expect(result.current.user).toEqual(user);
  });

  it("clears the session when the stored token is invalid", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue("jwt-bad");
    vi.mocked(authApi.getMe).mockRejectedValue(new Error("401"));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.status).toBe("unauthenticated"));
    expect(result.current.user).toBeNull();
  });

  it("logs in and becomes authenticated", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue(null);
    vi.mocked(authApi.login).mockResolvedValue(authResponse);

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.status).toBe("unauthenticated"));

    await act(async () => {
      await result.current.login({ email: "me@example.com", password: "password123" });
    });

    expect(authApi.login).toHaveBeenCalledWith({
      email: "me@example.com",
      password: "password123",
    });
    expect(result.current.status).toBe("authenticated");
    expect(result.current.user).toEqual(user);
  });

  it("registers and becomes authenticated", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue(null);
    vi.mocked(authApi.register).mockResolvedValue(authResponse);

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.status).toBe("unauthenticated"));

    await act(async () => {
      await result.current.register({ email: "me@example.com", password: "password123" });
    });

    expect(result.current.status).toBe("authenticated");
    expect(result.current.user).toEqual(user);
  });

  it("logs out, clearing the token and user", async () => {
    vi.mocked(tokenStore.getToken).mockReturnValue("jwt-abc");
    vi.mocked(authApi.getMe).mockResolvedValue(user);

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.status).toBe("authenticated"));

    act(() => result.current.logout());

    expect(tokenStore.clearToken).toHaveBeenCalled();
    expect(result.current.status).toBe("unauthenticated");
    expect(result.current.user).toBeNull();
  });
});

describe("useAuth", () => {
  it("throws when used outside an AuthProvider", () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      /useAuth must be used within an AuthProvider/,
    );
  });
});
