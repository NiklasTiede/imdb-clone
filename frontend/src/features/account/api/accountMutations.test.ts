import { accountMutationKeys } from "./accountMutations";

describe("accountMutationKeys", () => {
  it("uses the current profile query key for update invalidation", () => {
    expect(accountMutationKeys.currentProfile).toEqual([
      "account",
      "current-profile",
    ]);
  });
});
