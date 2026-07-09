import { apiGet, apiPost } from "./client";
import { setToken } from "@/lib/auth/tokenStore";
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from "@/lib/types/auth";

/** Register a new user and persist the returned JWT. */
export async function register(
  request: RegisterRequest,
): Promise<AuthResponse> {
  const response = await apiPost<AuthResponse>("/api/v1/auth/register", request);
  setToken(response.token);
  return response;
}

/** Authenticate and persist the returned JWT. */
export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await apiPost<AuthResponse>("/api/v1/auth/login", request);
  setToken(response.token);
  return response;
}

/** Fetch the currently authenticated user (requires a stored token). */
export function getMe(): Promise<UserResponse> {
  return apiGet<UserResponse>("/api/v1/auth/me");
}
