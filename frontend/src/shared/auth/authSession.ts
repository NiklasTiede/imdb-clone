const ACCESS_TOKEN_KEY = "jwtToken";
const ROLES_KEY = "rolesFromJwt";
const EXPIRES_AT_KEY = "jwtExpiresAt";
const USERNAME_KEY = "username";

export type AuthSessionData = {
  accessToken: string;
  roles?: string;
  username?: string;
  expiresAt?: number;
};

export const authSession = {
  getAccessToken(): string | null {
    return window.localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  setAccessToken(token: string): void {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
  },

  setSession(session: AuthSessionData): void {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
    if (session.roles) {
      window.localStorage.setItem(ROLES_KEY, session.roles);
    }
    if (session.username) {
      window.localStorage.setItem(USERNAME_KEY, session.username);
    }
    if (session.expiresAt !== undefined) {
      window.localStorage.setItem(EXPIRES_AT_KEY, session.expiresAt.toString());
    }
  },

  getUsername(): string | null {
    return window.localStorage.getItem(USERNAME_KEY);
  },

  setUsername(username: string): void {
    window.localStorage.setItem(USERNAME_KEY, username);
  },

  hasRole(role: string): boolean {
    return window.localStorage.getItem(ROLES_KEY)?.includes(role) ?? false;
  },

  isAuthenticated(): boolean {
    const jwtExpiresAt = window.localStorage.getItem(EXPIRES_AT_KEY);
    if (!jwtExpiresAt) {
      return false;
    }
    return Number.parseInt(jwtExpiresAt, 10) > Date.now() / 1000;
  },

  clear(): void {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(ROLES_KEY);
    window.localStorage.removeItem(EXPIRES_AT_KEY);
    window.localStorage.removeItem(USERNAME_KEY);
  },
};
