import { createPerformanceEventContext } from "../../../shared/observability/config";
import { reportPerformanceEvent } from "../../../shared/observability/performanceReporter";

export const reportHomeMovieOpen = ({
  feedInstanceId,
  movieId,
  position,
  sectionId,
}: {
  feedInstanceId: string;
  movieId: number;
  position: number;
  sectionId: string;
}) => {
  reportPerformanceEvent({
    context: createPerformanceEventContext("/"),
    feedInstanceId,
    movieId,
    name: "movie_open",
    position,
    sectionId,
    timestamp: performance.now(),
    type: "discovery_interaction",
  });
};
