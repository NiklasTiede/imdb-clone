import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { fontFamilies } from "../../../../theme";

const RatingsHeader = () => (
  <Stack>
    <Typography
      component="h1"
      sx={{ fontFamily: fontFamilies.display, fontSize: 24, fontWeight: 600 }}
    >
      Your ratings
    </Typography>
    <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
      Movies you've rated
    </Typography>
  </Stack>
);

export default RatingsHeader;
