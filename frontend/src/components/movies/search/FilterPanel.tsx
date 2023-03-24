import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import {
  Box,
  Checkbox,
  Container,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  SelectChangeEvent,
  Slider,
  styled,
} from "@mui/material";
import Typography from "@mui/material/Typography";
import {
  MovieMovieGenreEnum,
  MovieMovieTypeEnum,
} from "../../../client/movies/generator-output";
import { snakeToPascalCase } from "../MovieCard";

const minDistance = 1.0;

const marks = [
  { value: 0 },
  { value: 1 },
  { value: 2 },
  { value: 3 },
  { value: 4 },
  { value: 5 },
  { value: 6 },
  { value: 7 },
  { value: 8 },
  { value: 9 },
  { value: 10 },
];

const IOSSlider = styled(Slider)(({ theme }) => ({
  color: theme.palette.mode === "dark" ? "#3880ff" : "#3880ff",
  height: 2,
  padding: "15px 0",
  "& .MuiSlider-thumb": {
    height: 18,
    width: 18,
    backgroundColor: "#fff",
  },
  "& .MuiSlider-valueLabel": {
    fontSize: 12,
    fontWeight: "normal",
    top: -2,
    backgroundColor: "unset",
    color: theme.palette.text.primary,
    "&:before": {
      display: "none",
    },
    "& *": {
      background: "transparent",
      color: theme.palette.mode === "dark" ? "#fff" : "#000",
    },
  },
  "& .MuiSlider-track": {
    border: "none",
  },
  "& .MuiSlider-rail": {
    opacity: 0.5,
    backgroundColor: "#bfbfbf",
  },
  "& .MuiSlider-mark": {
    backgroundColor: "#bfbfbf",
    height: 6,
    width: 1,
    "&.MuiSlider-markActive": {
      opacity: 1,
      backgroundColor: "currentColor",
    },
  },
}));

const FilterPanel = () => {
  const navigateTo = useNavigate();

  const [ratingRange, setRatingRange] = useState<number[]>([0.0, 10.0]);

  const handleChange = (
    event: Event,
    newRatingRange: number | number[],
    activeThumb: number
  ) => {
    if (!Array.isArray(newRatingRange)) {
      return;
    }
    const [minRating, maxRating] = newRatingRange;
    if (maxRating - minRating < minDistance) {
      if (activeThumb === 0) {
        const clamped = Math.min(minRating, 10 - minDistance);
        setRatingRange([
          clamped,
          Math.round((clamped + minDistance) * 10) / 10,
        ]);
      } else {
        const clamped = Math.max(maxRating, minDistance);
        setRatingRange([
          Math.round((clamped - minDistance) * 10) / 10,
          clamped,
        ]);
      }
    } else {
      setRatingRange(newRatingRange as number[]);
    }
  };

  const [movieType, setMovieType] = useState("");

  const handleChange3 = (event: SelectChangeEvent<typeof movieType>) => {
    setMovieType(event.target.value);
  };

  // http://localhost:3000/filter

  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 4, marginTop: 10, fontSize: 18 }}>
            <Box sx={{ width: 200 }}>
              <Typography id="non-linear-slider" gutterBottom>
                FilterPanel <br />
                <br />
              </Typography>
              <IOSSlider
                aria-label="ios slider"
                value={ratingRange}
                min={0}
                step={0.1}
                max={10}
                onChange={handleChange}
                getAriaLabel={() => "Temperature range"}
                marks={marks}
                valueLabelDisplay="on"
              />

              <br />
              <br />

              <Box sx={{ minWidth: 50 }}>
                <FormControl fullWidth>
                  <InputLabel id="demo-simple-select-label">
                    Movie Type
                  </InputLabel>
                  <Select
                    labelId="demo-simple-select-label"
                    id="demo-simple-select"
                    value={movieType}
                    label="Movie Type"
                    onChange={handleChange3}
                  >
                    <MenuItem value={"None"}>None</MenuItem>
                    {Object.keys(MovieMovieTypeEnum).map(
                      (movieGenre: string) => (
                        <MenuItem value={movieGenre}>{movieGenre}</MenuItem>
                      )
                    )}
                  </Select>
                </FormControl>
              </Box>

              <FormControlLabel
                control={<Checkbox checked={true} name="gilad" />}
                label="Gilad Gray"
              />

              <Checkbox checked={true} name="Horror" />
              <Checkbox checked={false} name="Action" />
            </Box>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default FilterPanel;
