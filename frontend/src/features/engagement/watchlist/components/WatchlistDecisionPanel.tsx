import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import RefreshIcon from "@mui/icons-material/Refresh";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useMutation } from "@tanstack/react-query";
import { Link as RouterLink } from "react-router";
import { useState } from "react";
import { movieColors } from "../../../../theme";
import { recommendationApi } from "../../../../shared/api/moviesApi";
import { PosterMovieCard } from "../../../catalog";
import type {
  WatchlistInsights,
  WatchlistTonightRequestMoodEnum,
  WatchlistTonightResponse,
} from "../model/library";

type WatchlistDecisionPanelProps = {
  insights?: WatchlistInsights | undefined;
};

const moods: { label: string; value: WatchlistTonightRequestMoodEnum }[] = [
  { label: "Easy & warm", value: "LIGHT" },
  { label: "Edge of seat", value: "TENSE" },
  { label: "Big escape", value: "ESCAPIST" },
  { label: "Thoughtful", value: "THOUGHT_PROVOKING" },
];

const runtimeOptions = [
  { label: "Any length", value: undefined },
  { label: "Under 90 min", value: 90 },
  { label: "Under 2 hours", value: 120 },
];

const roleLabels = {
  SAFE_BET: "Safe bet",
  FORGOTTEN_GEM: "Forgotten gem",
  WILD_CARD: "Wild card",
} as const;

const WatchlistDecisionPanel = ({ insights }: WatchlistDecisionPanelProps) => {
  const [expanded, setExpanded] = useState(false);
  const [mood, setMood] = useState<WatchlistTonightRequestMoodEnum>();
  const [maxRuntimeMinutes, setMaxRuntimeMinutes] = useState<number>();
  const [shownMovieIds, setShownMovieIds] = useState<number[]>([]);
  const [response, setResponse] = useState<WatchlistTonightResponse>();
  const mutation = useMutation({
    mutationFn: async (
      request: Parameters<typeof recommendationApi.watchlistTonight>[0],
    ) => (await recommendationApi.watchlistTonight(request)).data,
    onSuccess: (result) => {
      setResponse(result);
      setShownMovieIds((current) => [
        ...current,
        ...(result.picks ?? []).flatMap((pick) => pick.movie?.id ?? []),
      ]);
    },
  });

  const choose = () => {
    setExpanded(true);
    mutation.mutate({
      ...(maxRuntimeMinutes ? { maxRuntimeMinutes } : {}),
      ...(mood ? { mood } : {}),
      excludedMovieIds: shownMovieIds,
      ...(response?.seed ? { seed: response.seed } : {}),
    });
  };

  const totalRuntime = insights?.totalRuntimeMinutes;
  const runtimeLabel =
    totalRuntime === undefined
      ? "—"
      : totalRuntime >= 60
        ? `${Math.floor(totalRuntime / 60)}h ${totalRuntime % 60}m`
        : `${totalRuntime}m`;

  return (
    <Paper
      component="section"
      variant="outlined"
      sx={{
        background: `linear-gradient(118deg, ${movieColors.surfaceElevated}, ${movieColors.surfaceInset})`,
        borderColor: "divider",
        borderRadius: 1,
        p: { xs: 2, sm: 2.5 },
      }}
    >
      <Stack spacing={2}>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1.5}
          sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
        >
          <Box>
            <Typography
              sx={{
                color: "primary.main",
                fontSize: 11,
                fontWeight: 800,
                letterSpacing: 1.1,
              }}
            >
              WATCHLIST TONIGHT
            </Typography>
            <Typography
              component="h2"
              sx={{ fontSize: 20, fontWeight: 800, mt: 0.25 }}
            >
              Find something new from your saved taste.
            </Typography>
            <Typography sx={{ color: "text.secondary", fontSize: 13, mt: 0.5 }}>
              {insights?.totalMovies ?? 0} saved movies guide the picks ·{" "}
              {runtimeLabel} total · {insights?.quickWatchCount ?? 0} quick
              watches
            </Typography>
          </Box>
          <Button
            disabled={mutation.isPending}
            onClick={choose}
            startIcon={
              mutation.isPending ? (
                <CircularProgress size={16} />
              ) : (
                <AutoAwesomeIcon />
              )
            }
            variant="contained"
          >
            {response ? "Show three others" : "Recommend something new"}
          </Button>
        </Stack>

        {expanded && (
          <Stack spacing={1.25}>
            <PickerRow
              label="What kind of night?"
              options={moods}
              selected={mood}
              onSelect={setMood}
            />
            <PickerRow
              label="How much time?"
              options={runtimeOptions}
              selected={maxRuntimeMinutes}
              onSelect={setMaxRuntimeMinutes}
            />
            <Stack direction="row" spacing={1}>
              <Button
                disabled={mutation.isPending}
                onClick={choose}
                size="small"
                startIcon={<RefreshIcon fontSize="small" />}
              >
                Refresh with these preferences
              </Button>
              <Button
                onClick={() => {
                  setMood(undefined);
                  setMaxRuntimeMinutes(undefined);
                }}
                size="small"
              >
                Reset preferences
              </Button>
            </Stack>
          </Stack>
        )}

        {response && <WatchlistTonightChoices response={response} />}
      </Stack>
    </Paper>
  );
};

const PickerRow = <T extends string | number>({
  label,
  onSelect,
  options,
  selected,
}: {
  label: string;
  onSelect: (value: T | undefined) => void;
  options: { label: string; value: T | undefined }[];
  selected: T | undefined;
}) => (
  <Stack
    direction="row"
    spacing={0.75}
    useFlexGap
    sx={{ alignItems: "center", flexWrap: "wrap" }}
  >
    <Typography sx={{ color: "text.secondary", fontSize: 12, minWidth: 118 }}>
      {label}
    </Typography>
    {options.map((option) => (
      <Chip
        color={selected === option.value ? "primary" : "default"}
        key={option.label}
        label={option.label}
        onClick={() =>
          onSelect(selected === option.value ? undefined : option.value)
        }
        size="small"
        variant={selected === option.value ? "filled" : "outlined"}
      />
    ))}
  </Stack>
);

const WatchlistTonightChoices = ({
  response,
}: {
  response: WatchlistTonightResponse;
}) => (
  <Box
    sx={{
      borderTop: "1px solid",
      borderColor: "divider",
      pt: 2,
    }}
  >
    <Box
      data-testid="watchlist-tonight-choices"
      sx={{
        display: "grid",
        gap: 1.5,
        gridTemplateColumns: {
          xs: "minmax(0, 1fr)",
          sm: "repeat(3, minmax(0, 166px))",
          md: "repeat(3, minmax(0, 184px))",
        },
        justifyContent: { sm: "center" },
      }}
    >
      {(response.picks ?? []).flatMap((pick) =>
        pick.movie
          ? [
              <Box key={pick.movie.id} sx={{ minWidth: 0, width: "100%" }}>
                <Typography
                  sx={{
                    color: "primary.main",
                    fontSize: 11,
                    fontWeight: 800,
                    mb: 0.5,
                    textTransform: "uppercase",
                  }}
                >
                  {pick.role ? roleLabels[pick.role] : "Tonight's pick"}
                </Typography>
                <PosterMovieCard movie={pick.movie} showImdbRating />
                <Typography
                  sx={{
                    color: "text.secondary",
                    fontSize: 12,
                    lineHeight: 1.45,
                    mt: 0.75,
                  }}
                >
                  {pick.explanation}
                </Typography>
                {pick.movie.id !== undefined && (
                  <Button
                    component={RouterLink}
                    size="small"
                    to={`/movie?id=${pick.movie.id}`}
                  >
                    Open movie
                  </Button>
                )}
              </Box>,
            ]
          : [],
      )}
    </Box>
  </Box>
);

export default WatchlistDecisionPanel;
