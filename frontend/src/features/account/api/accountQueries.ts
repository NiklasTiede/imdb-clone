import { accountApi } from "../../../shared/api/moviesApi";

export const accountQueryKeys = {
  currentProfile: ["account", "current-profile"] as const,
};

export const accountQueries = {
  currentProfile: () => ({
    queryFn: async () => {
      const response = await accountApi.getCurrentAccountProfile();
      return response.data;
    },
    queryKey: accountQueryKeys.currentProfile,
  }),
};
