import SortIcon from "@mui/icons-material/SortSharp";
import ViewListIcon from "@mui/icons-material/ViewListSharp";
import ViewModuleIcon from "@mui/icons-material/ViewModuleSharp";
import Button from "@mui/material/Button";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import type { MouseEvent } from "react";
import { useState } from "react";
import {
  type RatingSort,
  ratingSortLabels,
  type ScoreRange,
  scoreRanges,
} from "../utils/ratingsSorting";

export type RatingsView = "grid" | "list";

type RatingsToolbarProps = {
  scoreRange: ScoreRange;
  sortBy: RatingSort;
  view: RatingsView;
  onScoreRangeChange: (range: ScoreRange) => void;
  onSortChange: (sortBy: RatingSort) => void;
  onViewChange: (view: RatingsView) => void;
};

const sortOptions = Object.entries(ratingSortLabels) as Array<
  [RatingSort, string]
>;

const RatingsToolbar = ({
  scoreRange,
  sortBy,
  view,
  onScoreRangeChange,
  onSortChange,
  onViewChange,
}: RatingsToolbarProps) => {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={1}
      sx={{
        alignItems: { xs: "stretch", sm: "center" },
        borderBottom: "1px solid",
        borderColor: "divider",
        justifyContent: "space-between",
        pb: 1.5,
      }}
    >
      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
        {scoreRanges.map((range) => (
          <Button
            color={range.label === scoreRange.label ? "primary" : "inherit"}
            key={range.label}
            onClick={() => onScoreRangeChange(range)}
            size="small"
            variant={
              range.label === scoreRange.label ? "contained" : "outlined"
            }
          >
            {range.label}
          </Button>
        ))}
      </Stack>
      <Stack
        direction="row"
        spacing={1}
        sx={{ alignItems: "center", justifyContent: "flex-end" }}
      >
        <Button
          color="inherit"
          onClick={(event: MouseEvent<HTMLButtonElement>) =>
            setAnchorEl(event.currentTarget)
          }
          startIcon={<SortIcon />}
          variant="outlined"
        >
          {ratingSortLabels[sortBy]}
        </Button>
        <Menu
          anchorEl={anchorEl}
          onClose={() => setAnchorEl(null)}
          open={anchorEl !== null}
        >
          {sortOptions.map(([value, label]) => (
            <MenuItem
              key={value}
              selected={value === sortBy}
              onClick={() => {
                onSortChange(value);
                setAnchorEl(null);
              }}
            >
              {label}
            </MenuItem>
          ))}
        </Menu>
        <ToggleButtonGroup
          exclusive
          onChange={(_, nextView: RatingsView | null) => {
            if (nextView) {
              onViewChange(nextView);
            }
          }}
          size="small"
          value={view}
        >
          <ToggleButton aria-label="Grid view" value="grid">
            <ViewModuleIcon fontSize="small" />
          </ToggleButton>
          <ToggleButton aria-label="List view" value="list">
            <ViewListIcon fontSize="small" />
          </ToggleButton>
        </ToggleButtonGroup>
      </Stack>
    </Stack>
  );
};

export default RatingsToolbar;
