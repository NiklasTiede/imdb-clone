import type { CLSMetric } from "web-vitals";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { registerWebVitals } from "./webVitals";

vi.mock("web-vitals", () => ({
  onCLS: vi.fn(),
  onFCP: vi.fn(),
  onINP: vi.fn(),
  onLCP: vi.fn(),
  onTTFB: vi.fn(),
}));

const clsMetric = (): CLSMetric => ({
  delta: 12,
  entries: [],
  id: "CLS-id",
  name: "CLS",
  navigationType: "navigate",
  rating: "good",
  value: 123,
});

describe("registerWebVitals", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
    vi.clearAllMocks();
  });

  it("registers Web Vitals callbacks and reports normalized metric events", async () => {
    const webVitals = await import("web-vitals");
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    registerWebVitals();

    expect(webVitals.onCLS).toHaveBeenCalledTimes(1);
    const onCls = vi.mocked(webVitals.onCLS).mock.calls[0][0];
    onCls(clsMetric());

    expect(events).toEqual([
      expect.objectContaining({
        context: expect.objectContaining({
          appName: "imdb-clone-frontend",
          route: window.location.pathname,
        }),
        id: "CLS-id",
        name: "CLS",
        navigationType: "navigate",
        rating: "good",
        timestamp: expect.any(Number),
        type: "web_vital",
        value: 123,
      }),
    ]);
  });
});
