import StarBorderIcon from "@mui/icons-material/StarBorder";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link as RouterLink } from "react-router";

const EmptyRatings = () => (
  <Stack
    spacing={1.25}
    sx={{
      alignItems: "center",
      border: "1px dashed",
      borderColor: "divider",
      borderRadius: 1,
      color: "text.secondary",
      py: 6,
      textAlign: "center",
    }}
  >
    <StarBorderIcon sx={{ color: "#4dabf7", fontSize: 44 }} />
    <Typography component="h2" sx={{ color: "text.primary", fontWeight: 600 }}>
      No ratings yet
    </Typography>
    <Typography sx={{ maxWidth: 360 }}>
      Rate movies to start building your taste profile.
    </Typography>
    <Button component={RouterLink} to="/movie-search" variant="contained">
      Browse movies
    </Button>
  </Stack>
);

export default EmptyRatings;
