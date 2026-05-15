import CategoryIcon from "@mui/icons-material/CategorySharp";
import EventIcon from "@mui/icons-material/EventSharp";
import MovieIcon from "@mui/icons-material/MovieSharp";
import StraightenIcon from "@mui/icons-material/StraightenSharp";
import TuneIcon from "@mui/icons-material/TuneSharp";
import Button from "@mui/material/Button";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import type { SvgIconComponent } from "@mui/icons-material";
import { useState, type MouseEvent } from "react";
import { movieColors } from "../../../theme";
import {
  MovieSearchGenre,
  MovieSearchType,
  type MovieSearchGenre as MovieSearchGenreValue,
  type MovieSearchType as MovieSearchTypeValue,
} from "../../catalog";
import type { SearchUrlPatch } from "../utils/searchUrlState";

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
  movieGenre?: Set<MovieSearchGenreValue>;
  movieType?: MovieSearchTypeValue;
};

type MenuKey = "genre" | "runtime" | "type" | "year";

const activeSx = {
  backgroundColor: "rgba(77,171,247,0.14)",
  borderColor: "rgba(77,171,247,0.42)",
  color: "text.primary",
  "&:hover": {
    backgroundColor: "rgba(77,171,247,0.2)",
  },
};

const baseButtonSx = {
  borderColor: "rgba(255,255,255,0.14)",
  color: "text.secondary",
  minHeight: 32,
  px: 1.4,
  textTransform: "none",
  "&:hover": {
    backgroundColor: "rgba(255,255,255,0.05)",
    borderColor: "rgba(255,255,255,0.24)",
  },
};

const humanizeEnum = (value: string): string =>
  value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

const firstGenre = (filters: SearchFilters) =>
  Array.from(filters.movieGenre ?? [])[0];

const hasActiveFilters = (filters: SearchFilters): boolean =>
  Boolean(
    firstGenre(filters) ||
      filters.movieType ||
      filters.minStartYear ||
      filters.maxStartYear ||
      filters.minRuntimeMinutes ||
      filters.maxRuntimeMinutes,
  );

const yearLabel = (filters: SearchFilters): string => {
  const { maxStartYear, minStartYear } = filters;

  if (minStartYear && maxStartYear) {
    return `${minStartYear}-${maxStartYear}`;
  }
  if (minStartYear) {
    return `From ${minStartYear}`;
  }
  if (maxStartYear) {
    return `Until ${maxStartYear}`;
  }
  return "Any year";
};

const runtimeLabel = (filters: SearchFilters): string => {
  const { maxRuntimeMinutes, minRuntimeMinutes } = filters;

  if (minRuntimeMinutes && maxRuntimeMinutes) {
    return `${minRuntimeMinutes}-${maxRuntimeMinutes} min`;
  }
  if (minRuntimeMinutes) {
    return `${minRuntimeMinutes}+ min`;
  }
  if (maxRuntimeMinutes) {
    return `Under ${maxRuntimeMinutes} min`;
  }
  return "Any runtime";
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
    sx={{
      ...baseButtonSx,
      ...(active ? activeSx : {}),
    }}
  >
    {children}
  </Button>
);

const SearchFilterBar = ({
  filters,
  onChange,
  onClear,
}: SearchFilterBarProps) => {
  const [menu, setMenu] = useState<{
    anchor: HTMLElement;
    key: MenuKey;
  } | null>(null);
  const selectedGenre = firstGenre(filters);
  const hasFilters = hasActiveFilters(filters);

  const openMenu =
    (key: MenuKey) => (event: MouseEvent<HTMLButtonElement>) => {
      setMenu({ anchor: event.currentTarget, key });
    };

  const closeMenu = () => setMenu(null);

  const apply = (patch: SearchUrlPatch) => {
    closeMenu();
    onChange(patch);
  };

  return (
    <Stack
      direction="row"
      spacing={1}
      useFlexGap
      sx={{ flexWrap: "wrap" }}
    >
      <FilterButton
        active={Boolean(selectedGenre)}
        icon={CategoryIcon}
        onClick={openMenu("genre")}
      >
        {selectedGenre ? humanizeEnum(selectedGenre) : "All genres"}
      </FilterButton>

      <FilterButton
        active={Boolean(filters.minStartYear || filters.maxStartYear)}
        icon={EventIcon}
        onClick={openMenu("year")}
      >
        {yearLabel(filters)}
      </FilterButton>

      <FilterButton
        active={Boolean(filters.movieType)}
        icon={MovieIcon}
        onClick={openMenu("type")}
      >
        {filters.movieType ? humanizeEnum(filters.movieType) : "Movie & series"}
      </FilterButton>

      <FilterButton
        active={Boolean(filters.minRuntimeMinutes || filters.maxRuntimeMinutes)}
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
          sx={{
            color: "text.secondary",
            minHeight: 32,
            textTransform: "none",
          }}
        >
          Clear filters
        </Button>
      )}

      <Menu anchorEl={menu?.anchor} onClose={closeMenu} open={Boolean(menu)}>
        {menu?.key === "genre" && [
          <MenuItem key="all" onClick={() => apply({ genre: null })}>
            All genres
          </MenuItem>,
          ...Object.values(MovieSearchGenre).map((genre) => (
            <MenuItem key={genre} onClick={() => apply({ genre })}>
              {humanizeEnum(genre)}
            </MenuItem>
          )),
        ]}
        {menu?.key === "year" && [
          <MenuItem
            key="any"
            onClick={() => apply({ maxYear: null, minYear: null })}
          >
            Any year
          </MenuItem>,
          <MenuItem key="1990" onClick={() => apply({ minYear: 1990 })}>
            From 1990
          </MenuItem>,
          <MenuItem key="2000" onClick={() => apply({ minYear: 2000 })}>
            From 2000
          </MenuItem>,
          <MenuItem key="2010" onClick={() => apply({ minYear: 2010 })}>
            From 2010
          </MenuItem>,
          <MenuItem key="classic" onClick={() => apply({ maxYear: 1999 })}>
            Before 2000
          </MenuItem>,
        ]}
        {menu?.key === "type" && [
          <MenuItem key="all" onClick={() => apply({ type: null })}>
            Movie & series
          </MenuItem>,
          ...Object.values(MovieSearchType).map((type) => (
            <MenuItem key={type} onClick={() => apply({ type })}>
              {humanizeEnum(type)}
            </MenuItem>
          )),
        ]}
        {menu?.key === "runtime" && [
          <MenuItem
            key="any"
            onClick={() => apply({ maxRuntime: null, minRuntime: null })}
          >
            Any runtime
          </MenuItem>,
          <MenuItem key="short" onClick={() => apply({ maxRuntime: 90 })}>
            Under 90 min
          </MenuItem>,
          <MenuItem key="feature" onClick={() => apply({ minRuntime: 90 })}>
            90+ min
          </MenuItem>,
          <MenuItem key="long" onClick={() => apply({ minRuntime: 120 })}>
            120+ min
          </MenuItem>,
        ]}
      </Menu>
    </Stack>
  );
};

export default SearchFilterBar;
