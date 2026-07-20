import { afterEach, describe, expect, it } from "vitest";
import { clearToken, getToken, setToken } from "./tokenStore";

describe("tokenStore", () => {
  afterEach(() => window.localStorage.clear());

  it("returns null when no token is stored", () => {
    expect(getToken()).toBeNull();
  });

  it("stores and retrieves a token", () => {
    setToken("jwt-123");
    expect(getToken()).toBe("jwt-123");
  });

  it("overwrites an existing token", () => {
    setToken("jwt-old");
    setToken("jwt-new");
    expect(getToken()).toBe("jwt-new");
  });

  it("clears a stored token", () => {
    setToken("jwt-123");
    clearToken();
    expect(getToken()).toBeNull();
  });

  it("clearing when empty is a no-op", () => {
    expect(() => clearToken()).not.toThrow();
    expect(getToken()).toBeNull();
  });
});
