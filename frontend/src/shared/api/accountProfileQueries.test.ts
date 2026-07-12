import { accountQueries } from "./accountProfileQueries";

describe("accountQueries", () => {
  it("builds a stable shared current-profile query key", () => {
    expect(accountQueries.currentProfile().queryKey).toEqual([
      "account",
      "current-profile",
    ]);
  });
});
