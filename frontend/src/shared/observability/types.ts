export type PerformanceEventType =
  | "api_request"
  | "app_boot"
  | "browser_error"
  | "route_navigation"
  | "web_vital";

export type PerformanceEventContext = {
  appName: "imdb-clone-frontend";
  appVersion: string;
  environment: string;
  route?: string;
};

export type BasePerformanceEvent = {
  context: PerformanceEventContext;
  name: string;
  timestamp: number;
  type: PerformanceEventType;
  value?: number;
};

export type WebVitalPerformanceEvent = BasePerformanceEvent & {
  type: "web_vital";
  name: "CLS" | "FCP" | "INP" | "LCP" | "TTFB";
  id: string;
  navigationType?: string;
  rating?: "good" | "needs-improvement" | "poor";
  value: number;
};

export type AppBootPerformanceEvent = BasePerformanceEvent & {
  type: "app_boot";
  name: "app_boot";
  value: number;
};

export type RouteNavigationPerformanceEvent = BasePerformanceEvent & {
  type: "route_navigation";
  name: "route_navigation";
  from: string;
  to: string;
  value: number;
};

export type ApiRequestPerformanceEvent = BasePerformanceEvent & {
  type: "api_request";
  name: "api_request";
  failureKind?: "network" | "http" | "unknown";
  method: string;
  status?: number;
  success: boolean;
  url: string;
  value: number;
};

export type BrowserErrorPerformanceEvent = BasePerformanceEvent & {
  type: "browser_error";
  name: "browser_error" | "unhandled_rejection";
  column?: number;
  errorType?: string;
  line?: number;
  message: string;
  source?: string;
};

export type PerformanceEvent =
  | ApiRequestPerformanceEvent
  | AppBootPerformanceEvent
  | BrowserErrorPerformanceEvent
  | RouteNavigationPerformanceEvent
  | WebVitalPerformanceEvent;

export type PerformanceReporter = {
  report: (event: PerformanceEvent) => void;
};
