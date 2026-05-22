import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Link, MemoryRouter, Route, Routes } from "react-router";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import { RouteMetrics } from "./RouteMetrics";
import type { PerformanceEvent } from "./types";

describe("RouteMetrics", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports client route navigation duration", async () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <RouteMetrics />
        <Link to="/movie-search?q=test">Search</Link>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route path="/movie-search" element={<div>Search</div>} />
        </Routes>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole("link", { name: "Search" }));

    await waitFor(() => {
      expect(events).toEqual([
        expect.objectContaining({
          from: "/",
          name: "route_navigation",
          to: "/movie-search?q=test",
          type: "route_navigation",
        }),
      ]);
    });
  });
});
