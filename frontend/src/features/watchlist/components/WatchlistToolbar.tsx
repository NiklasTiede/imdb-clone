import SortIcon from "@mui/icons-material/SortSharp";
import ViewListIcon from "@mui/icons-material/ViewListSharp";
import ViewModuleIcon from "@mui/icons-material/ViewModuleSharp";
import Button from "@mui/material/Button";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import { MouseEvent, useState } from "react";
import { sortLabels, WatchlistSort } from "../utils/watchlistSorting";

export type WatchlistView = "grid" | "list";

type WatchlistToolbarProps = {
  sortBy: WatchlistSort;
  view: WatchlistView;
  onSortChange: (sortBy: WatchlistSort) => void;
  onViewChange: (view: WatchlistView) => void;
};

const sortOptions = Object.entries(sortLabels) as Array<[WatchlistSort, string]>;

const WatchlistToolbar = ({
  sortBy,
  view,
  onSortChange,
  onViewChange,
}: WatchlistToolbarProps) => {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  return (
    <Stack
      direction="row"
      spacing={1}
      sx={{
        alignItems: "center",
        borderBottom: "1px solid",
        borderColor: "divider",
        justifyContent: "flex-end",
        pb: 1.5,
      }}
    >
      <Button
        color="inherit"
        onClick={(event: MouseEvent<HTMLButtonElement>) =>
          setAnchorEl(event.currentTarget)
        }
        startIcon={<SortIcon />}
        variant="outlined"
      >
        {sortLabels[sortBy]}
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
        onChange={(_, nextView: WatchlistView | null) => {
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
  );
};

export default WatchlistToolbar;
