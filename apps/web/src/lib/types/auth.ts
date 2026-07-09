/**
 * Auth API contract types. Hand-written because the backend OpenAPI spec does not
 * yet expose the auth DTOs to `schema.d.ts`. Mirrors the backend records:
 * RegisterRequest / LoginRequest / AuthResponse / UserResponse.
 */
export interface UserResponse {
  id: string;
  email: string;
  displayName: string | null;
  role: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: UserResponse;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
