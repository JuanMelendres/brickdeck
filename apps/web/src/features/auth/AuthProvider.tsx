"use client";

import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  getMe,
  login as loginRequest,
  register as registerRequest,
} from "@/lib/api/auth";
import { clearToken, getToken } from "@/lib/auth/tokenStore";
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from "@/lib/types/auth";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

export interface AuthContextValue {
  user: UserResponse | null;
  status: AuthStatus;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  // Bootstrap the session from a stored token on mount.
  useEffect(() => {
    let active = true;
    const token = getToken();
    if (!token) {
      // The token lives in localStorage (client-only), so the initial state
      // cannot be derived during render/SSR — it must be resolved here on mount.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setStatus("unauthenticated");
      return;
    }
    getMe()
      .then((me) => {
        if (!active) return;
        setUser(me);
        setStatus("authenticated");
      })
      .catch(() => {
        if (!active) return;
        clearToken();
        setUser(null);
        setStatus("unauthenticated");
      });
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await loginRequest(request);
    setUser(response.user);
    setStatus("authenticated");
  }, []);

  const register = useCallback(async (request: RegisterRequest) => {
    const response = await registerRequest(request);
    setUser(response.user);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
