import "@testing-library/jest-dom/vitest";

// jsdom in this setup does not provide localStorage; supply an in-memory
// implementation so the token store and API client can be tested.
if (typeof window !== "undefined" && !window.localStorage) {
  const createMemoryStorage = (): Storage => {
    let store: Record<string, string> = {};
    return {
      get length() {
        return Object.keys(store).length;
      },
      clear: () => {
        store = {};
      },
      getItem: (key: string) => (key in store ? store[key] : null),
      key: (index: number) => Object.keys(store)[index] ?? null,
      removeItem: (key: string) => {
        delete store[key];
      },
      setItem: (key: string, value: string) => {
        store[key] = String(value);
      },
    } as Storage;
  };

  Object.defineProperty(window, "localStorage", {
    value: createMemoryStorage(),
    configurable: true,
  });
}
