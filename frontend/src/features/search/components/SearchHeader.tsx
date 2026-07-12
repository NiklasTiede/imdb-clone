import ViewListIcon from "@mui/icons-material/ViewListSharp";
import ViewModuleIcon from "@mui/icons-material/ViewModuleSharp";
import Stack from "@mui/material/Stack";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Typography from "@mui/material/Typography";

type SearchView = "grid" | "list";

type SearchHeaderProps = {
  onViewChange: (view: SearchView) => void;
  query: string | null;
  totalCount?: number | undefined;
  view: SearchView;
};

const SearchHeader = ({
  onViewChange,
  query,
  totalCount,
  view,
}: SearchHeaderProps) => {
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
