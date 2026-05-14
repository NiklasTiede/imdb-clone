import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

const RatingsHeader = () => (
  <Stack>
    <Typography component="h1" sx={{ fontSize: 24, fontWeight: 600 }}>
      Your Ratings
    </Typography>
    <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
      Movies you've rated
    </Typography>
  </Stack>
);

export default RatingsHeader;
