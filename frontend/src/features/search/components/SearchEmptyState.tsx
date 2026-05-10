import SearchOffIcon from "@mui/icons-material/SearchOff";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

const SearchEmptyState = () => (
  <Stack
    spacing={1}
    sx={{
      alignItems: "center",
      color: "text.secondary",
      py: { xs: 5, sm: 7 },
      textAlign: "center",
    }}
  >
    <SearchOffIcon sx={{ fontSize: 42 }} />
    <Typography
      component="h2"
      sx={{ color: "text.primary", fontSize: 18, fontWeight: 600 }}
    >
      No movies found
    </Typography>
    <Typography sx={{ fontSize: 14 }}>
      Try adjusting your search term or filters.
    </Typography>
  </Stack>
);

export default SearchEmptyState;
