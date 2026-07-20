import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import * as tokenStore from "@/lib/auth/tokenStore";
import { getMe, login, register } from "./auth";
import type { AuthResponse } from "@/lib/types/auth";

const authResponse: AuthResponse = {
  token: "jwt-abc",
  tokenType: "Bearer",
  user: {
    id: "u1",
    email: "me@example.com",
    displayName: "Me",
    role: "USER",
    createdAt: "2026-01-01T00:00:00",
  },
};

describe("register", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs the registration payload and stores the returned token", async () => {
    const post = vi.spyOn(client, "apiPost").mockResolvedValue(authResponse);
    const store = vi.spyOn(tokenStore, "setToken").mockImplementation(() => {});

    const result = await register({
      email: "me@example.com",
      password: "password123",
      displayName: "Me",
    });

    expect(post).toHaveBeenCalledWith("/api/v1/auth/register", {
      email: "me@example.com",
      password: "password123",
      displayName: "Me",
    });
    expect(store).toHaveBeenCalledWith("jwt-abc");
    expect(result).toBe(authResponse);
  });
});

describe("login", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs credentials and stores the returned token", async () => {
    const post = vi.spyOn(client, "apiPost").mockResolvedValue(authResponse);
    const store = vi.spyOn(tokenStore, "setToken").mockImplementation(() => {});

    const result = await login({
      email: "me@example.com",
      password: "password123",
    });

    expect(post).toHaveBeenCalledWith("/api/v1/auth/login", {
      email: "me@example.com",
      password: "password123",
    });
    expect(store).toHaveBeenCalledWith("jwt-abc");
    expect(result).toBe(authResponse);
  });
});

describe("getMe", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the current user and does not touch the token", async () => {
    const get = vi.spyOn(client, "apiGet").mockResolvedValue(authResponse.user);
    const store = vi.spyOn(tokenStore, "setToken").mockImplementation(() => {});

    const result = await getMe();

    expect(get).toHaveBeenCalledWith("/api/v1/auth/me");
    expect(store).not.toHaveBeenCalled();
    expect(result).toBe(authResponse.user);
  });
});
