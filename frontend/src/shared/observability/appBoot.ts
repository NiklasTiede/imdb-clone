import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

export const reportAppBoot = (
  entrypointStart: number,
  renderScheduledAt = performance.now(),
): void => {
  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    name: "app_boot",
    timestamp: renderScheduledAt,
    type: "app_boot",
    value: Math.max(0, renderScheduledAt - entrypointStart),
  });
};
