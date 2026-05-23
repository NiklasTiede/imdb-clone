import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Link, MemoryRouter, Route, Routes } from "react-router";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import { RouteMetrics } from "./RouteMetrics";
import type { PerformanceEvent } from "./types";

type TestRoutesProps = {
  onSearchClick?: () => void;
};

const TestRoutes = ({ onSearchClick }: TestRoutesProps) => (
  <MemoryRouter initialEntries={["/"]}>
    <RouteMetrics />
    <Link to="/movie-search?q=test" onClick={onSearchClick}>
      Search
    </Link>
    <Routes>
      <Route path="/" element={<div>Home</div>} />
      <Route path="/movie-search" element={<div>Search</div>} />
    </Routes>
  </MemoryRouter>
);

describe("RouteMetrics", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
    vi.restoreAllMocks();
  });

  it("reports client route navigation duration", async () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    render(<TestRoutes />);

    fireEvent.click(screen.getByRole("link", { name: "Search" }));

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

  it("measures from the navigation trigger instead of previous route idle time", async () => {
    const events: PerformanceEvent[] = [];
    let currentTime = 10_000;
    vi.spyOn(performance, "now").mockImplementation(() => currentTime);
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    render(
      <TestRoutes
        onSearchClick={() => {
          currentTime = 10_025;
        }}
      />,
    );

    fireEvent.click(screen.getByRole("link", { name: "Search" }));

    await waitFor(() => {
      expect(events).toEqual([
        expect.objectContaining({
          value: 25,
        }),
      ]);
    });
  });
});
