import { useSyncExternalStore } from "react";
import { authSession } from "./authSession";

export const useAuthSession = () =>
  useSyncExternalStore(
    authSession.subscribe,
    authSession.isAuthenticated,
    () => false,
  );
