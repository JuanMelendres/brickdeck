import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiGet, apiPost } from "./client";

const jsonResponse = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

describe("apiGet", () => {
  afterEach(() => vi.restoreAllMocks());

  it("requests the base URL + path and returns parsed JSON", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ hello: "world" }));

    const result = await apiGet<{ hello: string }>("/api/v1/ping");

    expect(result).toEqual({ hello: "world" });
    const calledUrl = fetchMock.mock.calls[0][0] as string;
    expect(calledUrl).toBe("http://localhost:8080/api/v1/ping");
  });

  it("appends query params, skipping undefined values", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ content: [] }));

    await apiGet("/api/v1/sets/search", { q: "star", page: 0, size: undefined });

    const calledUrl = fetchMock.mock.calls[0][0] as string;
    expect(calledUrl).toBe(
      "http://localhost:8080/api/v1/sets/search?q=star&page=0",
    );
  });

  it("throws ApiError with backend message on non-2xx", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ message: "Set not found" }, 404),
    );

    await expect(apiGet("/api/v1/sets/by-number/x")).rejects.toMatchObject({
      status: 404,
      message: "Set not found",
    });
    await expect(apiGet("/api/v1/sets/by-number/x")).rejects.toBeInstanceOf(
      ApiError,
    );
  });
});

describe("apiPost", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs to the base URL + path and returns parsed JSON", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ setNumber: "75257-1", linesProcessed: 42 }));

    const result = await apiPost<{ setNumber: string; linesProcessed: number }>(
      "/api/v1/catalog/sets/75257-1/inventory/import",
    );

    expect(result).toEqual({ setNumber: "75257-1", linesProcessed: 42 });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe(
      "http://localhost:8080/api/v1/catalog/sets/75257-1/inventory/import",
    );
    expect((init as RequestInit).method).toBe("POST");
  });

  it("throws ApiError with backend message on non-2xx", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ message: "Set not imported" }, 404),
    );

    await expect(
      apiPost("/api/v1/catalog/sets/x/inventory/import"),
    ).rejects.toMatchObject({ status: 404, message: "Set not imported" });
  });
});
