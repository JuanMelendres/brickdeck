"use client";

import { useContext } from "react";
import { AuthContext, type AuthContextValue } from "./AuthProvider";

/** Access the auth session. Must be used within an {@link AuthProvider}. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
