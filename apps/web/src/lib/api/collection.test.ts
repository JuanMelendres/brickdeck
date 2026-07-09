import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "./client";
import {
  addCollectionPart,
  addCollectionSet,
  listCollectionParts,
  listCollectionSets,
  removeCollectionPart,
  removeCollectionSet,
} from "./collection";
import type { PageResponse } from "@/lib/types/api";
import type {
  UserPartResponse,
  UserSetResponse,
} from "@/lib/types/collection";

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

describe("listCollectionParts", () => {
  afterEach(() => vi.restoreAllMocks());

  it("GETs the loose parts with pagination", async () => {
    const page: PageResponse<UserPartResponse> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    const spy = vi.spyOn(client, "apiGet").mockResolvedValue(page);

    const result = await listCollectionParts(0, 20);

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/parts", {
      page: 0,
      size: 20,
    });
    expect(result).toBe(page);
  });
});

describe("addCollectionPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("POSTs the add request", async () => {
    const spy = vi
      .spyOn(client, "apiPost")
      .mockResolvedValue({ id: "cp1" } as UserPartResponse);

    await addCollectionPart({
      externalPartNumber: "3001",
      colorExternalId: 4,
      quantity: 10,
    });

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/parts", {
      externalPartNumber: "3001",
      colorExternalId: 4,
      quantity: 10,
    });
  });
});

describe("removeCollectionPart", () => {
  afterEach(() => vi.restoreAllMocks());

  it("DELETEs by id", async () => {
    const spy = vi.spyOn(client, "apiDelete").mockResolvedValue(undefined);

    await removeCollectionPart("cp1");

    expect(spy).toHaveBeenCalledWith("/api/v1/collection/parts/cp1");
  });
});
