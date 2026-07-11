import { describe, expect, test } from "vitest";
import type { WatchlistItem } from "../model/watchlist";
import { pickRandomWatchlistItem } from "./pickRandomWatchlistItem";

const item = (movieId: number): WatchlistItem => ({ movieId });

describe("pickRandomWatchlistItem", () => {
  test("returns null for an empty watchlist", () => {
    expect(pickRandomWatchlistItem([])).toBeNull();
  });

  test("returns the only watchlist item", () => {
    const onlyItem = item(1);

    expect(pickRandomWatchlistItem([onlyItem])).toBe(onlyItem);
  });

  test("avoids the previously selected movie when another is available", () => {
    expect(pickRandomWatchlistItem([item(1), item(2)], 1)?.movieId).toBe(2);
  });

  test("falls back to the first item when every candidate is excluded", () => {
    const firstItem = item(1);

    expect(pickRandomWatchlistItem([firstItem, item(1)], 1)).toBe(firstItem);
  });
});
