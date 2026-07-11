import SortIcon from "@mui/icons-material/SortSharp";
import ViewListIcon from "@mui/icons-material/ViewListSharp";
import ViewModuleIcon from "@mui/icons-material/ViewModuleSharp";
import Button from "@mui/material/Button";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Typography from "@mui/material/Typography";
import { useState, type MouseEvent } from "react";
import type { SearchSort } from "../utils/searchUrlState";

type SearchView = "grid" | "list";

type SearchHeaderProps = {
  onSortChange: (sort: SearchSort) => void;
  onViewChange: (view: SearchView) => void;
  query: string | null;
  sort: SearchSort;
  totalCount?: number | undefined;
  view: SearchView;
};

const sortLabels: Record<SearchSort, string> = {
  rating_desc: "Highest rated",
  relevance: "Most relevant",
};

const SearchHeader = ({
  onSortChange,
  onViewChange,
  query,
  sort,
  totalCount,
  view,
}: SearchHeaderProps) => {
  const [sortAnchor, setSortAnchor] = useState<HTMLElement | null>(null);

  const handleSortOpen = (event: MouseEvent<HTMLButtonElement>) => {
    setSortAnchor(event.currentTarget);
  };

  const handleSortSelect = (nextSort: SearchSort) => {
    setSortAnchor(null);
    onSortChange(nextSort);
  };

  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={1.5}
      sx={{
        alignItems: { xs: "flex-start", sm: "center" },
        justifyContent: "space-between",
      }}
    >
      <Stack spacing={0.25}>
        <Typography component="h1" sx={{ fontSize: 22, fontWeight: 600 }}>
          {query ? `Results for "${query}"` : "Search movies"}
        </Typography>
        {totalCount !== undefined && (
          <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
            {totalCount} {totalCount === 1 ? "movie" : "movies"}
          </Typography>
        )}
      </Stack>

      <Stack
        direction="row"
        spacing={1}
        sx={{
          alignItems: "center",
          flexWrap: "wrap",
          justifyContent: { xs: "flex-start", sm: "flex-end" },
        }}
      >
        <Button
          aria-label={`Sort by ${sortLabels[sort]}`}
          onClick={handleSortOpen}
          size="small"
          startIcon={<SortIcon />}
          variant="outlined"
          sx={{
            borderColor: "rgba(255,255,255,0.12)",
            color: "text.primary",
            minHeight: 34,
            textTransform: "none",
          }}
        >
          {sortLabels[sort]}
        </Button>
        <Menu
          anchorEl={sortAnchor}
          onClose={() => setSortAnchor(null)}
          open={Boolean(sortAnchor)}
        >
          <MenuItem onClick={() => handleSortSelect("relevance")}>
            Most relevant
          </MenuItem>
          <MenuItem onClick={() => handleSortSelect("rating_desc")}>
            Highest rated
          </MenuItem>
        </Menu>

        <ToggleButtonGroup
          exclusive
          onChange={(_, nextView: SearchView | null) => {
            if (nextView) {
              onViewChange(nextView);
            }
          }}
          size="small"
          value={view}
          sx={{
            backgroundColor: "rgba(255,255,255,0.04)",
            border: "1px solid rgba(255,255,255,0.08)",
            borderRadius: 1,
            overflow: "hidden",
            "& .MuiToggleButton-root": {
              border: 0,
              borderRadius: 0,
              color: "text.secondary",
              px: 1.25,
            },
            "& .Mui-selected": {
              backgroundColor: "rgba(255,255,255,0.1)",
              color: "text.primary",
            },
          }}
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

export default SearchHeader;
