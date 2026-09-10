import { installLocalStorageMock } from "../../../test/installLocalStorageMock";
import { beforeEach, expect, it, vi } from "vitest";
import { readStreamingCountry, saveStreamingCountry } from "./streamingCountry";

beforeEach(installLocalStorageMock);

it("defaults to Switzerland independently of browser language and scopes saved choices by account", () => {
  expect(readStreamingCountry(null)).toBe("CH");
  saveStreamingCountry(null, "DE");
  saveStreamingCountry(7, "AT");
  expect(readStreamingCountry(null)).toBe("DE");
  expect(readStreamingCountry(7)).toBe("AT");
  expect(readStreamingCountry(8)).toBe("CH");
  saveStreamingCountry(7, "ZZ");
  expect(readStreamingCountry(7)).toBe("AT");
});

it("ignores corrupt storage and tolerates unavailable storage", () => {
  window.localStorage.setItem(
    "imdb-clone:streaming-country:guest",
    "not-a-country",
  );
  expect(readStreamingCountry(null)).toBe("CH");
  const get = vi
    .spyOn(window.localStorage, "getItem")
    .mockImplementation(() => {
      throw new Error("blocked");
    });
  const set = vi
    .spyOn(window.localStorage, "setItem")
    .mockImplementation(() => {
      throw new Error("blocked");
    });
  expect(readStreamingCountry(null)).toBe("CH");
  expect(() => saveStreamingCountry(null, "CH")).not.toThrow();
  get.mockRestore();
  set.mockRestore();
});
