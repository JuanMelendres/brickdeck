/**
 * Client-side JWT storage. The token is the source of identity for the stateless
 * backend (see docs/decisions/ADR-008). SSR-safe: no-ops when `window` /
 * `localStorage` is unavailable (server render).
 */
const TOKEN_KEY = "brickdeck.token";

function hasStorage(): boolean {
  return (
    typeof window !== "undefined" && typeof window.localStorage !== "undefined"
  );
}

export function getToken(): string | null {
  if (!hasStorage()) return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  if (!hasStorage()) return;
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  if (!hasStorage()) return;
  window.localStorage.removeItem(TOKEN_KEY);
}
