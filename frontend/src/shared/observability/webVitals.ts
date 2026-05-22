import { onCLS, onFCP, onINP, onLCP, onTTFB } from "web-vitals";
import type { MetricType } from "web-vitals";
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

type GlobalWebVitalsRegistration = typeof globalThis & {
  __imdbCloneWebVitalsRegistered?: boolean;
};

const getGlobalRegistration = (): GlobalWebVitalsRegistration =>
  globalThis as GlobalWebVitalsRegistration;

const reportWebVital = (metric: MetricType): void => {
  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    id: metric.id,
    name: metric.name,
    navigationType: metric.navigationType,
    rating: metric.rating,
    timestamp: performance.now(),
    type: "web_vital",
    value: metric.value,
  });
};

export const registerWebVitals = (): void => {
  const registration = getGlobalRegistration();

  if (registration.__imdbCloneWebVitalsRegistered) {
    return;
  }

  registration.__imdbCloneWebVitalsRegistered = true;

  onCLS(reportWebVital);
  onFCP(reportWebVital);
  onINP(reportWebVital);
  onLCP(reportWebVital);
  onTTFB(reportWebVital);
};

export const resetWebVitalsRegistrationForTests = (): void => {
  if (import.meta.env.MODE === "test") {
    delete getGlobalRegistration().__imdbCloneWebVitalsRegistered;
  }
};
