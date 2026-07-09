import { API_BASE_URL } from "@/lib/env";
import { clearToken, getToken } from "@/lib/auth/tokenStore";

export type ValidationErrors = Record<string, string>;

export class ApiError extends Error {
  readonly status: number;
  readonly validationErrors?: ValidationErrors;

  constructor(
    status: number,
    message: string,
    validationErrors?: ValidationErrors,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

export type QueryParams = Record<
  string,
  string | number | boolean | undefined | null
>;

function buildUrl(path: string, params?: QueryParams): string {
  const url = new URL(path, API_BASE_URL);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null) {
        url.searchParams.append(key, String(value));
      }
    }
  }
  return url.toString();
}

async function extractError(
  response: Response,
): Promise<{ message: string; validationErrors?: ValidationErrors }> {
  try {
    const body = (await response.json()) as {
      message?: string;
      validationErrors?: ValidationErrors;
    };
    if (body && typeof body.message === "string") {
      return { message: body.message, validationErrors: body.validationErrors };
    }
  } catch {
    // non-JSON error body
  }
  return { message: `Request failed with status ${response.status}` };
}

async function request<T>(url: string, init: RequestInit): Promise<T> {
  const token = getToken();
  const authHeader: Record<string, string> = token
    ? { Authorization: `Bearer ${token}` }
    : {};

  const response = await fetch(url, {
    ...init,
    headers: { Accept: "application/json", ...authHeader, ...init.headers },
  });

  if (!response.ok) {
    if (response.status === 401) {
      clearToken();
    }
    const { message, validationErrors } = await extractError(response);
    throw new ApiError(response.status, message, validationErrors);
  }

  return (await response.json()) as T;
}

export function apiGet<T>(path: string, params?: QueryParams): Promise<T> {
  return request<T>(buildUrl(path, params), { method: "GET" });
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(buildUrl(path), {
    method: "POST",
    headers:
      body === undefined ? {} : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
