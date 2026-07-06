import { API_BASE_URL } from "@/lib/env";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
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

async function extractMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    if (body && typeof body.message === "string") {
      return body.message;
    }
  } catch {
    // non-JSON error body
  }
  return `Request failed with status ${response.status}`;
}

async function request<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { Accept: "application/json", ...init.headers },
  });

  if (!response.ok) {
    throw new ApiError(response.status, await extractMessage(response));
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
