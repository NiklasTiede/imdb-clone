import { useSyncExternalStore } from "react";
import { authSession } from "./authSession";

export const useAuthSessionSnapshot = () =>
  useSyncExternalStore(
    authSession.subscribe,
    authSession.getSnapshot,
    authSession.getServerSnapshot,
  );

export const useAuthSession = () => useAuthSessionSnapshot().session !== null;
