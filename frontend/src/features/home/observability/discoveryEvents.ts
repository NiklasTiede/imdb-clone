import { createPerformanceEventContext } from "../../../shared/observability/config";
import { recordDiscoveryEvent } from "../../../shared/observability/discoveryEvents";
import { reportPerformanceEvent } from "../../../shared/observability/performanceReporter";

export const reportHomeMovieOpen = ({
  feedInstanceId,
  movieId,
  position,
  sectionId,
  strategyVersion,
}: {
  feedInstanceId: string;
  movieId: number;
  position: number;
  sectionId: string;
  strategyVersion: string;
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
  recordDiscoveryEvent({
    eventType: "MOVIE_OPEN",
    feedInstanceId,
    movieId,
    position,
    sectionId,
    strategyVersion,
  });
};

export const reportHomeSectionImpression = ({
  feedInstanceId,
  sectionId,
  strategyVersion,
}: {
  feedInstanceId: string;
  sectionId: string;
  strategyVersion: string;
}) => {
  recordDiscoveryEvent({
    eventType: "SECTION_IMPRESSION",
    feedInstanceId,
    position: 0,
    sectionId,
    strategyVersion,
  });
};

export const reportHomeWatchlistAdded = ({
  feedInstanceId,
  movieId,
  sectionId,
  strategyVersion,
}: {
  feedInstanceId: string;
  movieId: number;
  sectionId: string;
  strategyVersion: string;
}) => {
  recordDiscoveryEvent({
    eventType: "WATCHLIST_ADDED",
    feedInstanceId,
    movieId,
    position: 0,
    sectionId,
    strategyVersion,
  });
};
