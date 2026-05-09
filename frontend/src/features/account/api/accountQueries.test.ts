import { accountQueries } from "./accountQueries";

describe("accountQueries", () => {
  it("builds a stable current profile query key", () => {
    expect(accountQueries.currentProfile().queryKey).toEqual([
      "account",
      "current-profile",
    ]);
  });
});
