const ACCESS_TOKEN_KEY = "jwtToken";
const ROLES_KEY = "rolesFromJwt";
const EXPIRES_AT_KEY = "jwtExpiresAt";
const USERNAME_KEY = "username";

export const authSession = {
  getAccessToken(): string | null {
    return window.localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  setAccessToken(token: string): void {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
  },

  clear(): void {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(ROLES_KEY);
    window.localStorage.removeItem(EXPIRES_AT_KEY);
    window.localStorage.removeItem(USERNAME_KEY);
  },
};
