import { useEffect, useRef } from "react";
import { useLocation } from "react-router";
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

const STALE_NAVIGATION_START_MS = 5_000;

const toRoute = (pathname: string, search: string): string =>
  `${pathname}${search}`;

export const RouteMetrics = () => {
  const location = useLocation();
  const currentRoute = toRoute(location.pathname, location.search);
  const previousRouteRef = useRef(currentRoute);
  const pendingNavigationStartRef = useRef<number | null>(null);

  useEffect(() => {
    const markNavigationStart = () => {
      pendingNavigationStartRef.current = performance.now();
    };

    document.addEventListener("click", markNavigationStart, { capture: true });
    window.addEventListener("popstate", markNavigationStart);

    return () => {
      document.removeEventListener("click", markNavigationStart, {
        capture: true,
      });
      window.removeEventListener("popstate", markNavigationStart);
    };
  }, []);

  useEffect(() => {
    const previousRoute = previousRouteRef.current;

    if (previousRoute !== currentRoute) {
      const now = performance.now();
      const navigationStart = pendingNavigationStartRef.current;
      const value =
        navigationStart !== null &&
        now - navigationStart <= STALE_NAVIGATION_START_MS
          ? now - navigationStart
          : 0;
      reportPerformanceEvent({
        context: createPerformanceEventContext(location.pathname),
        from: previousRoute,
        name: "route_navigation",
        timestamp: now,
        to: currentRoute,
        type: "route_navigation",
        value: Math.max(0, value),
      });
      previousRouteRef.current = currentRoute;
      pendingNavigationStartRef.current = null;
    }
  }, [currentRoute, location.pathname]);

  return null;
};
