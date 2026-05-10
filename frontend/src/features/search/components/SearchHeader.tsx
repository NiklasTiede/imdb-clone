import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

type SearchHeaderProps = {
  query: string | null;
  totalCount?: number;
};

const SearchHeader = ({ query, totalCount }: SearchHeaderProps) => (
  <Stack
    direction={{ xs: "column", sm: "row" }}
    spacing={1}
    sx={{
      alignItems: { xs: "flex-start", sm: "baseline" },
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
  </Stack>
);

export default SearchHeader;
