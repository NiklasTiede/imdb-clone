import TuneIcon from "@mui/icons-material/Tune";
import {
  Box,
  Button,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Slider,
  Stack,
  Typography,
} from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { MovieSearchGenre, MovieSearchType } from "../../catalog";
import { movieColors } from "../../../theme";
import AppSurface from "../../../shared/layout/AppSurface";
import PageContent from "../../../shared/layout/PageContent";
import PageHeader from "../../../shared/layout/PageHeader";

const yearMarks = [
  { label: "1950", value: 1950 },
  { label: "1980", value: 1980 },
  { label: "2010", value: 2010 },
  { label: "2026", value: 2026 },
];

const humanizeEnum = (value: string): string =>
  value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

const FilterPanelPage = () => {
  const navigate = useNavigate();
  const [genre, setGenre] = useState("");
  const [movieType, setMovieType] = useState("");
  const [minYear, setMinYear] = useState(1990);
  const [sort, setSort] = useState("rating_desc");

  const selectedSummary = useMemo(() => {
    const items = [
      genre ? humanizeEnum(genre) : "Any genre",
      movieType ? humanizeEnum(movieType) : "Any format",
      `From ${minYear}`,
    ];
    return items.join(" / ");
  }, [genre, minYear, movieType]);

  const handleSubmit = () => {
    const params = new URLSearchParams();

    if (genre) {
      params.set("genre", genre);
    }
    if (movieType) {
      params.set("type", movieType);
    }
    if (minYear > 1950) {
      params.set("minYear", String(minYear));
    }
    if (sort) {
      params.set("sort", sort);
    }

    void navigate(`/movie-search?${params.toString()}`);
  };

  return (
    <PageContent maxWidth="1120px">
      <Stack spacing={2.5}>
        <PageHeader
          eyebrow="Advanced search"
          title="Find your next movie"
          subtitle="Shape the catalog by release window, genre, and format."
        />

        <Grid container spacing={2.5}>
          <Grid size={{ xs: 12, md: 7 }}>
            <AppSurface accent="brand" sx={{ p: { xs: 2, sm: 3 } }}>
              <Stack spacing={3}>
                <Stack
                  direction="row"
                  spacing={1.25}
                  sx={{ alignItems: "center" }}
                >
                  <Box
                    sx={{
                      alignItems: "center",
                      backgroundColor: "rgba(245,197,24,0.14)",
                      border: "1px solid rgba(245,197,24,0.24)",
                      borderRadius: 1,
                      color: movieColors.brand,
                      display: "inline-flex",
                      height: 38,
                      justifyContent: "center",
                      width: 38,
                    }}
                  >
                    <TuneIcon fontSize="small" />
                  </Box>
                  <Box>
                    <Typography sx={{ fontSize: 18, fontWeight: 700 }}>
                      Filter catalog
                    </Typography>
                    <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
                      Start broad, then narrow down from the results page.
                    </Typography>
                  </Box>
                </Stack>

                <Box>
                  <Typography
                    component="label"
                    htmlFor="min-year"
                    sx={{
                      display: "block",
                      fontSize: 13,
                      fontWeight: 600,
                      mb: 1,
                    }}
                  >
                    Minimum start year
                  </Typography>
                  <Slider
                    aria-label="Minimum start year"
                    id="min-year"
                    marks={yearMarks}
                    max={2026}
                    min={1950}
                    onChange={(_event, value: number | number[]) =>
                      setMinYear(
                        Array.isArray(value) ? (value[0] ?? 1950) : value,
                      )
                    }
                    step={1}
                    value={minYear}
                    valueLabelDisplay="auto"
                    sx={{
                      color: movieColors.brand,
                      maxWidth: 620,
                      "& .MuiSlider-rail": {
                        color: "rgba(255,255,255,0.28)",
                      },
                      "& .MuiSlider-thumb": {
                        border: `2px solid ${movieColors.surface}`,
                      },
                    }}
                  />
                </Box>

                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FormControl fullWidth>
                      <InputLabel id="genre-filter-label">Genre</InputLabel>
                      <Select
                        label="Genre"
                        labelId="genre-filter-label"
                        onChange={(event: SelectChangeEvent) =>
                          setGenre(event.target.value)
                        }
                        value={genre}
                      >
                        <MenuItem value="">Any genre</MenuItem>
                        {Object.values(MovieSearchGenre).map((item) => (
                          <MenuItem key={item} value={item}>
                            {humanizeEnum(item)}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FormControl fullWidth>
                      <InputLabel id="movie-type-filter-label">
                        Movie type
                      </InputLabel>
                      <Select
                        label="Movie type"
                        labelId="movie-type-filter-label"
                        onChange={(event: SelectChangeEvent) =>
                          setMovieType(event.target.value)
                        }
                        value={movieType}
                      >
                        <MenuItem value="">Any format</MenuItem>
                        {Object.values(MovieSearchType).map((item) => (
                          <MenuItem key={item} value={item}>
                            {humanizeEnum(item)}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                </Grid>

                <FormControl fullWidth>
                  <InputLabel id="sort-filter-label">Sort results</InputLabel>
                  <Select
                    label="Sort results"
                    labelId="sort-filter-label"
                    onChange={(event: SelectChangeEvent) =>
                      setSort(event.target.value)
                    }
                    value={sort}
                  >
                    <MenuItem value="rating_desc">Highest IMDb rating</MenuItem>
                    <MenuItem value="">Backend default</MenuItem>
                  </Select>
                </FormControl>

                <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
                  <Button onClick={handleSubmit} variant="contained">
                    Search movies
                  </Button>
                  <Button
                    onClick={() => {
                      setGenre("");
                      setMovieType("");
                      setMinYear(1990);
                      setSort("rating_desc");
                    }}
                    variant="outlined"
                  >
                    Reset
                  </Button>
                </Stack>
              </Stack>
            </AppSurface>
          </Grid>

          <Grid size={{ xs: 12, md: 5 }}>
            <AppSurface
              accent="info"
              sx={{ p: { xs: 2, sm: 3 }, height: "100%" }}
            >
              <Stack
                spacing={2}
                sx={{ height: "100%", justifyContent: "space-between" }}
              >
                <Stack spacing={1}>
                  <Typography sx={{ fontSize: 16, fontWeight: 700 }}>
                    Current search shape
                  </Typography>
                  <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
                    {selectedSummary}
                  </Typography>
                </Stack>
                <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
                  These filters feed the same results grid as the topbar search,
                  keeping discovery in one place.
                </Typography>
              </Stack>
            </AppSurface>
          </Grid>
        </Grid>
      </Stack>
    </PageContent>
  );
};

export default FilterPanelPage;
