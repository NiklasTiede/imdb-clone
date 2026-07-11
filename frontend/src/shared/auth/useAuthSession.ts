import { useSyncExternalStore } from "react";
import { authSession } from "./authSession";

const subscribe = (listener: () => void) => authSession.subscribe(listener);
const getSnapshot = () => authSession.getSnapshot();
const getServerSnapshot = () => authSession.getServerSnapshot();

export const useAuthSessionSnapshot = () =>
  useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

export const useAuthSession = () => useAuthSessionSnapshot().session !== null;
