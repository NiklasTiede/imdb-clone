import type {
  TonightModeRequest,
  TonightModeRequestEraEnum,
  TonightModeRequestMoodEnum,
  TonightModeRequestMovieGenresEnum,
} from "../../../client/movies/generator-output";
import { recommendationApi } from "../../../shared/api/moviesApi";
import type { Movie } from "../../catalog";

export type TonightOptions = Pick<
  TonightModeRequest,
  "era" | "includeWatched" | "maxRuntimeMinutes" | "mood" | "movieGenres" | "movieType"
> & {
  excludedMovieIds: number[];
  seed?: string;
};

export type TonightMood = TonightModeRequestMoodEnum;
export type TonightEra = TonightModeRequestEraEnum;
export type TonightGenre = TonightModeRequestMovieGenresEnum;

export type TonightPick = {
  explanation: string;
  movie: Movie;
};

export type TonightResult = {
  picks: TonightPick[];
  seed?: string;
};

export const getTonightPicks = async (options: TonightOptions): Promise<TonightResult> => {
  const response = await recommendationApi.tonight(options);
  return {
    picks: (response.data.picks ?? []).flatMap((pick) =>
      pick.movie ? [{ explanation: pick.explanation ?? "A strong fit for your evening.", movie: pick.movie as Movie }] : [],
    ),
    ...(response.data.seed ? { seed: response.data.seed } : {}),
  };
};
