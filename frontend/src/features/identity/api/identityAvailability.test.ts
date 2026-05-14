import { authApi } from "../../../shared/api/moviesApi";
import {
  checkEmailAvailability,
  checkUsernameAvailability,
} from "./identityAvailability";

vi.mock("../../../shared/api/moviesApi", () => ({
  authApi: {
    checkEmailAvailability: vi.fn(),
    checkUsernameAvailability: vi.fn(),
  },
}));

describe("identity availability api", () => {
  it("checks username availability through the auth api", async () => {
    vi.mocked(authApi.checkUsernameAvailability).mockResolvedValueOnce({
      data: { isAvailable: true },
    } as Awaited<ReturnType<typeof authApi.checkUsernameAvailability>>);

    await expect(checkUsernameAvailability("les_grossman")).resolves.toEqual({
      isAvailable: true,
    });
    expect(authApi.checkUsernameAvailability).toHaveBeenCalledWith(
      "les_grossman",
    );
  });

  it("checks email availability through the auth api", async () => {
    vi.mocked(authApi.checkEmailAvailability).mockResolvedValueOnce({
      data: { isAvailable: false },
    } as Awaited<ReturnType<typeof authApi.checkEmailAvailability>>);

    await expect(checkEmailAvailability("tom@example.com")).resolves.toEqual({
      isAvailable: false,
    });
    expect(authApi.checkEmailAvailability).toHaveBeenCalledWith(
      "tom@example.com",
    );
  });
});
