import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorderSharp";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";

const EmptyWatchlist = () => (
  <Stack
    spacing={1.5}
    sx={{
      alignItems: "center",
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 1,
      p: { xs: 4, sm: 6 },
      textAlign: "center",
    }}
  >
    <BookmarkBorderIcon sx={{ color: "text.secondary", fontSize: 48 }} />
    <Typography component="h2" sx={{ fontSize: 20, fontWeight: 600 }}>
      Your watchlist is empty
    </Typography>
    <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
      Bookmark movies to save them for later
    </Typography>
    <Button component={Link} to="/movie-search" variant="contained">
      Browse movies
    </Button>
  </Stack>
);

export default EmptyWatchlist;
