import {
  configurePerformanceReporter,
  reportPerformanceEvent,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent, PerformanceReporter } from "./types";

const createEvent = (): PerformanceEvent => ({
  context: {
    appName: "imdb-clone-frontend",
    appVersion: "test",
    environment: "test",
  },
  name: "app_boot",
  timestamp: 100,
  type: "app_boot",
  value: 42,
});

describe("performanceReporter", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("forwards events to the configured reporter", () => {
    const reportedEvents: PerformanceEvent[] = [];
    const reporter: PerformanceReporter = {
      report: (event) => {
        reportedEvents.push(event);
      },
    };

    configurePerformanceReporter(reporter);
    reportPerformanceEvent(createEvent());

    expect(reportedEvents).toEqual([createEvent()]);
  });

  it("swallows reporter failures so observability cannot break the app", () => {
    const warning = vi
      .spyOn(console, "warn")
      .mockImplementation(() => undefined);
    const reporter: PerformanceReporter = {
      report: () => {
        throw new Error("collector unavailable");
      },
    };

    configurePerformanceReporter(reporter);

    expect(() => reportPerformanceEvent(createEvent())).not.toThrow();
    expect(warning).toHaveBeenCalledWith(
      "[frontend-observability] reporter failed",
      expect.any(Error),
    );
  });

  it("swallows async reporter failures without unhandled rejections", async () => {
    const warning = vi
      .spyOn(console, "warn")
      .mockImplementation(() => undefined);
    const unhandledRejections: unknown[] = [];
    const trackUnhandledRejection = (reason: unknown): void => {
      unhandledRejections.push(reason);
    };
    const reporter: PerformanceReporter = {
      report: () => Promise.reject(new Error("collector unavailable")),
    };

    process.on("unhandledRejection", trackUnhandledRejection);

    try {
      configurePerformanceReporter(reporter);

      expect(() => reportPerformanceEvent(createEvent())).not.toThrow();

      await Promise.resolve();

      expect(unhandledRejections).toEqual([]);
      expect(warning).toHaveBeenCalledWith(
        "[frontend-observability] reporter failed",
        expect.any(Error),
      );
    } finally {
      process.off("unhandledRejection", trackUnhandledRejection);
    }
  });
});
