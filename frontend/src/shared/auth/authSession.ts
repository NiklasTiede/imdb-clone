import * as zod from "zod";

const listeners = new Set<() => void>();

const accountSessionResponseSchema = zod.object({
  id: zod.number().int().positive(),
  username: zod.string().min(1),
  email: zod.email(),
  roles: zod.array(zod.string().min(1)),
});

export type AccountSessionResponse = zod.infer<
  typeof accountSessionResponseSchema
>;

export type AuthSessionData = AccountSessionResponse;

export const parseAccountSessionResponse = (
  value: unknown,
): AccountSessionResponse => accountSessionResponseSchema.parse(value);

export type AuthSessionSnapshot = {
  bootstrapped: boolean;
  session: AuthSessionData | null;
};

let currentSession: AuthSessionData | null = null;
let bootstrapped = false;
let currentSnapshot: AuthSessionSnapshot = {
  bootstrapped,
  session: currentSession,
};

const notifyListeners = (): void => {
  listeners.forEach((listener) => listener());
};

const refreshSnapshot = (): void => {
  currentSnapshot = {
    bootstrapped,
    session: currentSession,
  };
};

const normalizeRoles = (roles: string[] | undefined): string[] =>
  roles?.filter(Boolean) ?? [];

export const authSession = {
  getSnapshot(): AuthSessionSnapshot {
    return currentSnapshot;
  },

  getServerSnapshot(): AuthSessionSnapshot {
    return {
      bootstrapped: false,
      session: null,
    };
  },

  markBootstrapStarted(): void {
    bootstrapped = false;
    refreshSnapshot();
    notifyListeners();
  },

  completeBootstrap(session: AuthSessionData | null): void {
    currentSession = session;
    bootstrapped = true;
    refreshSnapshot();
    notifyListeners();
  },

  setSession(session: AuthSessionData): void {
    currentSession = {
      ...session,
      roles: normalizeRoles(session.roles),
    };
    bootstrapped = true;
    refreshSnapshot();
    notifyListeners();
  },

  getUsername(): string | null {
    return currentSession?.username ?? null;
  },

  setUsername(username: string): void {
    if (currentSession) {
      currentSession = { ...currentSession, username };
      refreshSnapshot();
    }
    notifyListeners();
  },

  hasRole(role: string): boolean {
    return currentSession?.roles?.includes(role) ?? false;
  },

  isAuthenticated(): boolean {
    return currentSession !== null;
  },

  isBootstrapped(): boolean {
    return bootstrapped;
  },

  clear(): void {
    currentSession = null;
    bootstrapped = true;
    refreshSnapshot();
    notifyListeners();
  },

  resetForTests(): void {
    currentSession = null;
    bootstrapped = false;
    refreshSnapshot();
    notifyListeners();
  },

  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};
