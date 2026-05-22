import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { registerBrowserErrorReporting } from "./browserErrors";

describe("registerBrowserErrorReporting", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports global error events without serializing the full error object", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    const cleanup = registerBrowserErrorReporting();
    window.dispatchEvent(
      new ErrorEvent("error", {
        colno: 7,
        error: new TypeError("boom"),
        filename: "http://localhost/src/example.ts",
        lineno: 4,
        message: "boom",
      }),
    );
    cleanup();

    expect(events).toEqual([
      expect.objectContaining({
        column: 7,
        errorType: "TypeError",
        line: 4,
        message: "boom",
        name: "browser_error",
        source: "http://localhost/src/example.ts",
        type: "browser_error",
      }),
    ]);
    expect(JSON.stringify(events[0])).not.toContain("stack");
  });

  it("reports unhandled promise rejections with a coarse message", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    const cleanup = registerBrowserErrorReporting();
    const event = new Event("unhandledrejection") as PromiseRejectionEvent;
    Object.defineProperty(event, "reason", {
      value: new Error("rejected"),
    });
    window.dispatchEvent(event);
    cleanup();

    expect(events).toEqual([
      expect.objectContaining({
        errorType: "Error",
        message: "rejected",
        name: "unhandled_rejection",
        type: "browser_error",
      }),
    ]);
  });
});
