import { useEffect, useRef } from "react";
import { useLocation } from "react-router";
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

const toRoute = (pathname: string, search: string): string =>
  `${pathname}${search}`;

export const RouteMetrics = () => {
  const location = useLocation();
  const currentRoute = toRoute(location.pathname, location.search);
  const previousRouteRef = useRef(currentRoute);
  const navigationStartRef = useRef(performance.now());

  useEffect(() => {
    const previousRoute = previousRouteRef.current;

    if (previousRoute !== currentRoute) {
      const now = performance.now();
      reportPerformanceEvent({
        context: createPerformanceEventContext(location.pathname),
        from: previousRoute,
        name: "route_navigation",
        timestamp: now,
        to: currentRoute,
        type: "route_navigation",
        value: Math.max(0, now - navigationStartRef.current),
      });
      previousRouteRef.current = currentRoute;
    }

    navigationStartRef.current = performance.now();
  }, [currentRoute, location.pathname]);

  return null;
};
