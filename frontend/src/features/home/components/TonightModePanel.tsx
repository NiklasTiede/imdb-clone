import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import RefreshIcon from "@mui/icons-material/Refresh";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Typography from "@mui/material/Typography";
import { useMutation } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { movieColors } from "../../../theme";
import { PosterMovieCard } from "../../catalog";
import {
  getTonightPicks,
  type TonightEra,
  type TonightGenre,
  type TonightMood,
  type TonightPick,
} from "../api/tonightMode";

type TonightModePanelProps = { watchedMovieIds?: Set<number> };

const moods: { label: string; value: TonightMood }[] = [
  { label: "Easy & warm", value: "LIGHT" },
  { label: "Edge of seat", value: "TENSE" },
  { label: "Big escape", value: "ESCAPIST" },
  { label: "Something to chew on", value: "THOUGHT_PROVOKING" },
  { label: "Romantic", value: "ROMANTIC" },
];
const runtimes = [
  { label: "Any length", value: undefined },
  { label: "Under 90 min", value: 90 },
  { label: "Under 2 hours", value: 120 },
  { label: "A long one", value: 180 },
];
const eras: { label: string; value: TonightEra | undefined }[] = [
  { label: "Any era", value: undefined },
  { label: "Classic", value: "CLASSIC" },
  { label: "90s", value: "NINETIES" },
  { label: "Modern", value: "MODERN" },
];
const genres: { label: string; value: TonightGenre }[] = [
  { label: "Comedy", value: "COMEDY" },
  { label: "Drama", value: "DRAMA" },
  { label: "Horror", value: "HORROR" },
  { label: "Sci-fi", value: "SCI_FI" },
  { label: "Thriller", value: "THRILLER" },
];

const TonightModePanel = ({
  watchedMovieIds = new Set(),
}: TonightModePanelProps) => {
  const [era, setEra] = useState<TonightEra>();
  const [includeWatched, setIncludeWatched] = useState(false);
  const [mood, setMood] = useState<TonightMood>();
  const [maxRuntimeMinutes, setMaxRuntimeMinutes] = useState<number>();
  const [selectedGenres, setSelectedGenres] = useState<Set<TonightGenre>>(
    new Set(),
  );
  const [shownMovieIds, setShownMovieIds] = useState<Set<number>>(new Set());
  const [picks, setPicks] = useState<TonightPick[]>([]);
  const [seed, setSeed] = useState<string>();

  const mutation = useMutation({
    mutationFn: getTonightPicks,
    onSuccess: (result) => {
      setPicks(result.picks);
      setSeed(result.seed);
      setShownMovieIds(
        (current) =>
          new Set([
            ...current,
            ...result.picks.flatMap((pick) => pick.movie.id ?? []),
          ]),
      );
    },
  });

  const excludedMovieIds = useMemo(
    () => [...shownMovieIds, ...(includeWatched ? [] : watchedMovieIds)],
    [includeWatched, shownMovieIds, watchedMovieIds],
  );

  const findPicks = () =>
    mutation.mutate({
      ...(era ? { era } : {}),
      excludedMovieIds,
      includeWatched,
      ...(maxRuntimeMinutes ? { maxRuntimeMinutes } : {}),
      ...(mood ? { mood } : {}),
      movieGenres: selectedGenres,
      movieType: "MOVIE",
      ...(seed ? { seed } : {}),
    });

  const toggleGenre = (genre: TonightGenre) =>
    setSelectedGenres((current) => {
      const next = new Set(current);
      if (next.has(genre)) next.delete(genre);
      else next.add(genre);
      return next;
    });

  return (
    <Box
      component="section"
      aria-labelledby="tonight-mode-title"
      sx={{
        background: `linear-gradient(118deg, ${movieColors.surfaceElevated}, ${movieColors.surfaceInset})`,
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 2,
        mb: 5,
        overflow: "hidden",
        p: { xs: 2.25, sm: 3 },
        position: "relative",
      }}
    >
      <Box
        aria-hidden
        sx={{
          background:
            "radial-gradient(circle at 94% 8%, rgba(245,197,24,.18), transparent 26%)",
          inset: 0,
          pointerEvents: "none",
          position: "absolute",
        }}
      />
      <Stack spacing={2.25} sx={{ position: "relative" }}>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1.25}
          sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
        >
          <Box>
            <Stack
              direction="row"
              spacing={0.75}
              sx={{ alignItems: "center", mb: 0.5 }}
            >
              <AutoAwesomeIcon color="primary" fontSize="small" />
              <Typography
                sx={{
                  color: "primary.main",
                  fontSize: 11,
                  fontWeight: 800,
                  letterSpacing: 1.25,
                }}
              >
                TONIGHT MODE
              </Typography>
            </Stack>
            <Typography
              component="h2"
              id="tonight-mode-title"
              variant="h5"
              sx={{ fontWeight: 700 }}
            >
              Three good answers for tonight.
            </Typography>
            <Typography sx={{ color: "text.secondary", fontSize: 14, mt: 0.5 }}>
              Give us a little context; we&apos;ll make the hard part easier.
            </Typography>
          </Box>
          <Stack direction="row" sx={{ alignItems: "center" }}>
            <Switch
              checked={includeWatched}
              onChange={(_event, checked) => setIncludeWatched(checked)}
            />
            <Typography sx={{ fontSize: 12, maxWidth: 150 }}>
              Include movies already on my list
            </Typography>
          </Stack>
        </Stack>

        <PickerRow
          label="What kind of night?"
          options={moods}
          selected={mood}
          onSelect={setMood}
        />
        <PickerRow
          label="How much time?"
          options={runtimes}
          selected={maxRuntimeMinutes}
          onSelect={setMaxRuntimeMinutes}
        />
        <PickerRow
          label="When was it made?"
          options={eras}
          selected={era}
          onSelect={setEra}
        />
        <Stack
          direction="row"
          spacing={0.75}
          useFlexGap
          sx={{ alignItems: "center", flexWrap: "wrap" }}
        >
          <Typography
            sx={{ color: "text.secondary", fontSize: 12, minWidth: 118 }}
          >
            Optional genres
          </Typography>
          {genres.map((genre) => (
            <Chip
              key={genre.value}
              label={genre.label}
              onClick={() => toggleGenre(genre.value)}
              size="small"
              color={selectedGenres.has(genre.value) ? "primary" : "default"}
              variant={selectedGenres.has(genre.value) ? "filled" : "outlined"}
            />
          ))}
        </Stack>

        {mutation.isError && (
          <Alert severity="error">
            We couldn&apos;t find tonight&apos;s picks. Please try again.
          </Alert>
        )}
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Button
            disabled={mutation.isPending}
            onClick={findPicks}
            startIcon={
              mutation.isPending ? (
                <CircularProgress size={16} />
              ) : (
                <AutoAwesomeIcon />
              )
            }
            variant="contained"
          >
            {picks.length > 0
              ? "Show me three others"
              : "Pick tonight's movies"}
          </Button>
          {picks.length > 0 && (
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              We keep the shown picks out of the next round.
            </Typography>
          )}
        </Stack>

        {picks.length > 0 && (
          <TonightChoices
            picks={picks}
            onRefresh={findPicks}
            refreshing={mutation.isPending}
          />
        )}
      </Stack>
    </Box>
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
        key={option.label}
        label={option.label}
        onClick={() =>
          onSelect(selected === option.value ? undefined : option.value)
        }
        size="small"
        color={selected === option.value ? "primary" : "default"}
        variant={selected === option.value ? "filled" : "outlined"}
      />
    ))}
  </Stack>
);

const TonightChoices = ({
  onRefresh,
  picks,
  refreshing,
}: {
  onRefresh: () => void;
  picks: TonightPick[];
  refreshing: boolean;
}) => (
  <Box sx={{ borderTop: "1px solid", borderColor: "divider", pt: 2.25 }}>
    {picks.length === 0 ? (
      <Alert severity="info">
        No close matches yet—try a longer runtime or a wider mood.
      </Alert>
    ) : (
      <>
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            justifyContent: "space-between",
            mb: 1.25,
          }}
        >
          <Typography sx={{ fontSize: 13, fontWeight: 800 }}>
            Your three choices
          </Typography>
          {refreshing ? (
            <Stack direction="row" spacing={0.75} sx={{ alignItems: "center" }}>
              <CircularProgress size={14} />
              <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
                Finding three more…
              </Typography>
            </Stack>
          ) : (
            <Button
              onClick={onRefresh}
              size="small"
              startIcon={<RefreshIcon fontSize="small" />}
            >
              Different three
            </Button>
          )}
        </Stack>
        <Box
          aria-label="Tonight's movie choices"
          component="ul"
          data-testid="tonight-mode-choices"
          sx={{
            display: "grid",
            gap: { xs: 1.75, sm: 2 },
            gridAutoColumns: { xs: "144px", sm: "auto" },
            gridAutoFlow: { xs: "column", sm: "row" },
            gridTemplateColumns: {
              xs: "none",
              sm: "repeat(3, minmax(0, 1fr))",
            },
            justifyContent: { xs: "start", sm: "center" },
            listStyle: "none",
            maxWidth: { sm: 632 },
            mx: { xs: -1.5, sm: "auto" },
            my: 0,
            overflowX: { xs: "auto", sm: "visible" },
            overscrollBehaviorX: "contain",
            pb: { xs: 1.5, sm: 0 },
            pt: { xs: 1.5, sm: 0 },
            px: { xs: 1.5, sm: 0 },
            scrollPaddingInline: { xs: 12, sm: 0 },
            scrollSnapType: { xs: "x mandatory", sm: "none" },
            scrollbarColor: `${movieColors.brand} transparent`,
            scrollbarWidth: "thin",
            WebkitOverflowScrolling: "touch",
          }}
        >
          {picks.slice(0, 3).map((pick) => (
            <Box
              component="li"
              key={pick.movie.id}
              sx={{
                minWidth: 0,
                position: "relative",
                scrollSnapAlign: { xs: "start", sm: "none" },
                "&:hover, &:focus-within": { zIndex: 1 },
              }}
            >
              <PosterMovieCard movie={pick.movie} />
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
            </Box>
          ))}
        </Box>
      </>
    )}
  </Box>
);

export default TonightModePanel;
