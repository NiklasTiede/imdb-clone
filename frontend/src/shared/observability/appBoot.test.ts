import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { reportAppBoot } from "./appBoot";

describe("reportAppBoot", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports elapsed time since entrypoint startup", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    reportAppBoot(10, 60);

    expect(events).toEqual([
      expect.objectContaining({
        name: "app_boot",
        timestamp: 60,
        type: "app_boot",
        value: 50,
      }),
    ]);
  });
});
