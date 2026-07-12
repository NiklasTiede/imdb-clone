import CategoryIcon from "@mui/icons-material/CategorySharp";
import CloseIcon from "@mui/icons-material/CloseSharp";
import EventIcon from "@mui/icons-material/EventSharp";
import StraightenIcon from "@mui/icons-material/StraightenSharp";
import TuneIcon from "@mui/icons-material/TuneSharp";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Drawer from "@mui/material/Drawer";
import IconButton from "@mui/material/IconButton";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { SvgIconComponent } from "@mui/icons-material";
import { useState, type MouseEvent } from "react";
import type { MovieSearchGenre } from "../../catalog";
import { movieColors } from "../../../theme";
import type { SearchUrlPatch } from "../utils/searchUrlState";
import {
  findRangePreset,
  humanizeSearchValue,
  RUNTIME_PRESETS,
  SEARCHABLE_MOVIE_GENRES,
  YEAR_PRESETS,
  type SearchRangePreset,
} from "../utils/searchFilterOptions";

type SearchFilterBarProps = {
  filters: SearchFilters;
  onChange: (patch: SearchUrlPatch) => void;
  onClear: () => void;
};

type SearchFilters = {
  maxRuntimeMinutes?: number;
  maxStartYear?: number;
  minRuntimeMinutes?: number;
  minStartYear?: number;
  movieGenre?: Set<MovieSearchGenre>;
};

type MenuKey = "genre" | "runtime" | "year";

type MobileFilterDraft = {
  genre: MovieSearchGenre | null;
  maxRuntime: number | null;
  maxYear: number | null;
  minRuntime: number | null;
  minYear: number | null;
};

const activeSx = {
  backgroundColor: "rgba(122,184,255,0.14)",
  borderColor: "rgba(122,184,255,0.48)",
  color: "text.primary",
  "&:hover": {
    backgroundColor: "rgba(122,184,255,0.22)",
  },
};

const baseButtonSx = {
  borderColor: "rgba(255,255,255,0.14)",
  color: "text.secondary",
  minHeight: 34,
  px: 1.4,
  textTransform: "none",
  "&:hover": {
    backgroundColor: "rgba(255,255,255,0.05)",
    borderColor: "rgba(255,255,255,0.24)",
  },
};

const drawerChoiceSx = {
  borderColor: "rgba(255,255,255,0.12)",
  color: "text.secondary",
  justifyContent: "flex-start",
  minHeight: 38,
  px: 1.25,
  textAlign: "left",
  textTransform: "none",
  "&[aria-pressed='true']": {
    backgroundColor: "rgba(245,197,24,0.14)",
    borderColor: "rgba(245,197,24,0.48)",
    color: "text.primary",
  },
};

const firstGenre = (filters: SearchFilters) =>
  Array.from(filters.movieGenre ?? [])[0];

const hasRange = (min: number | undefined, max: number | undefined) =>
  min !== undefined || max !== undefined;

const hasActiveFilters = (filters: SearchFilters): boolean =>
  Boolean(
    firstGenre(filters) ||
      hasRange(filters.minStartYear, filters.maxStartYear) ||
      hasRange(filters.minRuntimeMinutes, filters.maxRuntimeMinutes),
  );

const countActiveFilters = (filters: SearchFilters): number =>
  [
    Boolean(firstGenre(filters)),
    hasRange(filters.minStartYear, filters.maxStartYear),
    hasRange(filters.minRuntimeMinutes, filters.maxRuntimeMinutes),
  ].filter(Boolean).length;

const rangeLabel = (
  min: number | undefined,
  max: number | undefined,
  unit = "",
): string => {
  if (min !== undefined && max !== undefined) {
    return `${min}–${max}${unit}`;
  }
  if (min !== undefined) {
    return `From ${min}${unit}`;
  }
  return `Until ${max}${unit}`;
};

const yearLabel = (filters: SearchFilters): string => {
  const preset = findRangePreset(
    YEAR_PRESETS,
    filters.minStartYear,
    filters.maxStartYear,
  );

  return preset
    ? preset.label
    : hasRange(filters.minStartYear, filters.maxStartYear)
      ? rangeLabel(filters.minStartYear, filters.maxStartYear)
      : "Any era";
};

const runtimeLabel = (filters: SearchFilters): string => {
  const preset = findRangePreset(
    RUNTIME_PRESETS,
    filters.minRuntimeMinutes,
    filters.maxRuntimeMinutes,
  );

  return preset
    ? preset.label
    : hasRange(filters.minRuntimeMinutes, filters.maxRuntimeMinutes)
      ? rangeLabel(
          filters.minRuntimeMinutes,
          filters.maxRuntimeMinutes,
          " min",
        )
      : "Any length";
};

const FilterButton = ({
  active,
  children,
  icon: Icon,
  onClick,
}: {
  active?: boolean;
  children: string;
  icon: SvgIconComponent;
  onClick: (event: MouseEvent<HTMLButtonElement>) => void;
}) => (
  <Button
    onClick={onClick}
    size="small"
    startIcon={<Icon sx={{ fontSize: 16 }} />}
    variant="outlined"
    sx={{ ...baseButtonSx, ...(active ? activeSx : {}) }}
  >
    {children}
  </Button>
);

const DrawerChoiceSection = ({
  label,
  onSelect,
  options,
  selected,
}: {
  label: string;
  onSelect: (value: string | null) => void;
  options: readonly { label: string; value: string }[];
  selected: string | null;
}) => (
  <Stack spacing={1}>
    <Typography sx={{ fontSize: 13, fontWeight: 800 }}>{label}</Typography>
    <Stack spacing={0.75}>
      <Button
        aria-pressed={selected === null}
        onClick={() => onSelect(null)}
        size="small"
        variant="outlined"
        sx={drawerChoiceSx}
      >
        Any {label.toLowerCase()}
      </Button>
      {options.map((option) => (
        <Button
          aria-pressed={selected === option.value}
          key={option.value}
          onClick={() => onSelect(option.value)}
          size="small"
          variant="outlined"
          sx={drawerChoiceSx}
        >
          {option.label}
        </Button>
      ))}
    </Stack>
  </Stack>
);

const toMobileDraft = (filters: SearchFilters): MobileFilterDraft => ({
  genre: firstGenre(filters) ?? null,
  maxRuntime: filters.maxRuntimeMinutes ?? null,
  maxYear: filters.maxStartYear ?? null,
  minRuntime: filters.minRuntimeMinutes ?? null,
  minYear: filters.minStartYear ?? null,
});

const rangeValue = (preset: SearchRangePreset | undefined): string | null =>
  preset ? `${preset.min ?? ""}:${preset.max ?? ""}` : null;

const selectedPresetValue = (
  presets: readonly SearchRangePreset[],
  min: number | null,
  max: number | null,
): string | null => rangeValue(findRangePreset(presets, min ?? undefined, max ?? undefined));

const presetFromValue = (
  presets: readonly SearchRangePreset[],
  value: string | null,
): SearchRangePreset | undefined =>
  presets.find((preset) => rangeValue(preset) === value);

const SearchFilterBar = ({
  filters,
  onChange,
  onClear,
}: SearchFilterBarProps) => {
  const [menu, setMenu] = useState<{
    anchor: HTMLElement;
    key: MenuKey;
  } | null>(null);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [mobileDraft, setMobileDraft] = useState<MobileFilterDraft>(() =>
    toMobileDraft(filters),
  );
  const selectedGenre = firstGenre(filters);
  const hasFilters = hasActiveFilters(filters);
  const activeFilterCount = countActiveFilters(filters);

  const openMenu =
    (key: MenuKey) => (event: MouseEvent<HTMLButtonElement>) => {
      setMenu({ anchor: event.currentTarget, key });
    };

  const closeMenu = () => setMenu(null);

  const apply = (patch: SearchUrlPatch) => {
    closeMenu();
    onChange(patch);
  };

  const applyYearPreset = (preset?: SearchRangePreset) =>
    apply({ maxYear: preset?.max ?? null, minYear: preset?.min ?? null });

  const applyRuntimePreset = (preset?: SearchRangePreset) =>
    apply({
      maxRuntime: preset?.max ?? null,
      minRuntime: preset?.min ?? null,
    });

  const openMobileDrawer = () => {
    setMobileDraft(toMobileDraft(filters));
    setMobileDrawerOpen(true);
  };

  const applyMobileFilters = () => {
    onChange({
      genre: mobileDraft.genre,
      maxRuntime: mobileDraft.maxRuntime,
      maxYear: mobileDraft.maxYear,
      minRuntime: mobileDraft.minRuntime,
      minYear: mobileDraft.minYear,
    });
    setMobileDrawerOpen(false);
  };

  const genreOptions = SEARCHABLE_MOVIE_GENRES.map((genre) => ({
    label: humanizeSearchValue(genre),
    value: genre,
  }));
  const yearOptions = YEAR_PRESETS.map((preset) => ({
    label: preset.label,
    value: rangeValue(preset) ?? "",
  }));
  const runtimeOptions = RUNTIME_PRESETS.map((preset) => ({
    label: preset.label,
    value: rangeValue(preset) ?? "",
  }));

  return (
    <Stack spacing={1.25}>
      <Stack
        direction="row"
        spacing={1}
        useFlexGap
        sx={{ display: { xs: "none", sm: "flex" }, flexWrap: "wrap" }}
      >
        <FilterButton
          active={Boolean(selectedGenre)}
          icon={CategoryIcon}
          onClick={openMenu("genre")}
        >
          {selectedGenre ? humanizeSearchValue(selectedGenre) : "All genres"}
        </FilterButton>

        <FilterButton
          active={hasRange(filters.minStartYear, filters.maxStartYear)}
          icon={EventIcon}
          onClick={openMenu("year")}
        >
          {yearLabel(filters)}
        </FilterButton>

        <FilterButton
          active={hasRange(filters.minRuntimeMinutes, filters.maxRuntimeMinutes)}
          icon={StraightenIcon}
          onClick={openMenu("runtime")}
        >
          {runtimeLabel(filters)}
        </FilterButton>

        {hasFilters && (
          <Button
            onClick={onClear}
            size="small"
            startIcon={<TuneIcon sx={{ color: movieColors.brand }} />}
            sx={{ color: "text.secondary", minHeight: 34, textTransform: "none" }}
          >
            Clear all
          </Button>
        )}
      </Stack>

      <Box sx={{ display: { sm: "none" } }}>
        <Button
          aria-label={`Filters${activeFilterCount ? `, ${activeFilterCount} active` : ""}`}
          onClick={openMobileDrawer}
          size="small"
          startIcon={<TuneIcon sx={{ fontSize: 17 }} />}
          variant="outlined"
          sx={{ ...baseButtonSx, ...(hasFilters ? activeSx : {}) }}
        >
          Filters{activeFilterCount ? ` (${activeFilterCount})` : ""}
        </Button>
      </Box>

      {hasFilters && (
        <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: "wrap" }}>
          {selectedGenre && (
            <Chip
              label={humanizeSearchValue(selectedGenre)}
              onDelete={() => onChange({ genre: null })}
              size="small"
              sx={{ backgroundColor: "rgba(122,184,255,0.12)", color: "text.primary" }}
            />
          )}
          {hasRange(filters.minStartYear, filters.maxStartYear) && (
            <Chip
              label={yearLabel(filters)}
              onDelete={() => onChange({ maxYear: null, minYear: null })}
              size="small"
              sx={{ backgroundColor: "rgba(122,184,255,0.12)", color: "text.primary" }}
            />
          )}
          {hasRange(filters.minRuntimeMinutes, filters.maxRuntimeMinutes) && (
            <Chip
              label={runtimeLabel(filters)}
              onDelete={() =>
                onChange({ maxRuntime: null, minRuntime: null })
              }
              size="small"
              sx={{ backgroundColor: "rgba(122,184,255,0.12)", color: "text.primary" }}
            />
          )}
        </Stack>
      )}

      <Menu anchorEl={menu?.anchor} onClose={closeMenu} open={Boolean(menu)}>
        {menu?.key === "genre" && [
          <MenuItem key="all" onClick={() => apply({ genre: null })}>
            All genres
          </MenuItem>,
          ...SEARCHABLE_MOVIE_GENRES.map((genre) => (
            <MenuItem key={genre} onClick={() => apply({ genre })}>
              {humanizeSearchValue(genre)}
            </MenuItem>
          )),
        ]}
        {menu?.key === "year" && [
          <MenuItem key="any" onClick={() => applyYearPreset()}>
            Any era
          </MenuItem>,
          ...YEAR_PRESETS.map((preset) => (
            <MenuItem key={preset.label} onClick={() => applyYearPreset(preset)}>
              {preset.label}
            </MenuItem>
          )),
        ]}
        {menu?.key === "runtime" && [
          <MenuItem key="any" onClick={() => applyRuntimePreset()}>
            Any length
          </MenuItem>,
          ...RUNTIME_PRESETS.map((preset) => (
            <MenuItem
              key={preset.label}
              onClick={() => applyRuntimePreset(preset)}
            >
              {preset.label}
            </MenuItem>
          )),
        ]}
      </Menu>

      <Drawer
        anchor="bottom"
        onClose={() => setMobileDrawerOpen(false)}
        open={mobileDrawerOpen}
        slotProps={{
          paper: {
            sx: {
              borderTop: "1px solid rgba(255,255,255,0.12)",
              borderTopLeftRadius: 16,
              borderTopRightRadius: 16,
              maxHeight: "85dvh",
            },
          },
        }}
      >
        <Stack spacing={2.25} sx={{ overflowY: "auto", p: 2 }}>
          <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
            <Box>
              <Typography sx={{ fontSize: 18, fontWeight: 800 }}>Filter movies</Typography>
              <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
                Pick what fits your mood and time.
              </Typography>
            </Box>
            <IconButton aria-label="Close filters" onClick={() => setMobileDrawerOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Stack>
          <Divider />
          <DrawerChoiceSection
            label="Genre"
            onSelect={(value) =>
              setMobileDraft((draft) => ({
                ...draft,
                genre: value as MovieSearchGenre | null,
              }))
            }
            options={genreOptions}
            selected={mobileDraft.genre}
          />
          <DrawerChoiceSection
            label="Era"
            onSelect={(value) => {
              const preset = presetFromValue(YEAR_PRESETS, value);
              setMobileDraft((draft) => ({
                ...draft,
                maxYear: preset?.max ?? null,
                minYear: preset?.min ?? null,
              }));
            }}
            options={yearOptions}
            selected={selectedPresetValue(
              YEAR_PRESETS,
              mobileDraft.minYear,
              mobileDraft.maxYear,
            )}
          />
          <DrawerChoiceSection
            label="Runtime"
            onSelect={(value) => {
              const preset = presetFromValue(RUNTIME_PRESETS, value);
              setMobileDraft((draft) => ({
                ...draft,
                maxRuntime: preset?.max ?? null,
                minRuntime: preset?.min ?? null,
              }));
            }}
            options={runtimeOptions}
            selected={selectedPresetValue(
              RUNTIME_PRESETS,
              mobileDraft.minRuntime,
              mobileDraft.maxRuntime,
            )}
          />
          <Stack direction="row" spacing={1}>
            <Button
              onClick={() => {
                onClear();
                setMobileDrawerOpen(false);
              }}
              variant="outlined"
              sx={{ flex: 1 }}
            >
              Clear all
            </Button>
            <Button onClick={applyMobileFilters} sx={{ flex: 1 }} variant="contained">
              Show results
            </Button>
          </Stack>
        </Stack>
      </Drawer>
    </Stack>
  );
};

export default SearchFilterBar;
