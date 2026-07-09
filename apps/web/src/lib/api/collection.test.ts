import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import {
  addCollectionSet,
  listCollectionSets,
  removeCollectionSet,
} from "./collection";
import type { PageResponse } from "@/lib/types/api";
import type { UserSetResponse } from "@/lib/types/collection";

describe("listCollectionSets", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the collection sets with pagination", async () => {
    const page: PageResponse<UserSetResponse> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    const result = await listCollectionSets(1, 20);

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/sets", {
      page: 1,
      size: 20,
    });
    expect(result).toBe(page);
  });
});

describe("addCollectionSet", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs the add request", async () => {
    const spy = vi
      .spyOn(client, "apiPost")
      .mockResolvedValue({ id: "cs1" } as UserSetResponse);

    await addCollectionSet({ setNumber: "75257-1", status: "OWNED" });

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/sets", {
      setNumber: "75257-1",
      status: "OWNED",
    });
  });
});

describe("removeCollectionSet", () => {
  afterEach(() => vi.restoreAllMocks());

  it("DELETEs by id", async () => {
    const spy = vi.spyOn(client, "apiDelete").mockResolvedValue(undefined);

    await removeCollectionSet("cs1");

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/sets/cs1");
  });
});
