import { readRenamedStorage } from "../../../shared/storage/readRenamedStorage";

// Curated ISO country selection; the order keeps Switzerland and its neighbours first.
const COUNTRIES = new Set([
  "CH",
  "AT",
  "DE",
  "IT",
  "FR",
  "GB",
  "ES",
  "PT",
  "NL",
  "BE",
  "IE",
  "SE",
  "NO",
  "PL",
  "US",
  "CA",
  "AU",
  "BR",
  "IN",
  "MX",
]);
const labels = new Intl.DisplayNames(["en"], { type: "region" });
export const streamingCountries = [...COUNTRIES].map((code) => ({
  code,
  name: labels.of(code) ?? code,
}));

const key = (accountId: number | null) =>
  `popcorn-society:streaming-country:${accountId === null ? "guest" : `account-${accountId}`}`;

export const readStreamingCountry = (accountId: number | null): string => {
  try {
    const value = readRenamedStorage(
      window.localStorage, key(accountId),
      key(accountId).replace("popcorn-society:", "imdb-clone:"),
      (candidate) => COUNTRIES.has(candidate),
    );
    return value && COUNTRIES.has(value) ? value : "CH";
  } catch {
    return "CH";
  }
};

export const saveStreamingCountry = (
  accountId: number | null,
  country: string,
) => {
  if (!COUNTRIES.has(country)) return;
  try {
    window.localStorage.setItem(key(accountId), country);
  } catch {
    // The in-memory selection still works when storage is unavailable.
  }
};
