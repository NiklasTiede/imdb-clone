import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

type HomeFeedEndStateProps = {
  onDiscoverNewMix: () => void;
};

const HomeFeedEndState = ({ onDiscoverNewMix }: HomeFeedEndStateProps) => (
  <Stack
    spacing={1.5}
    sx={{ alignItems: "center", pb: { xs: 5, md: 7 }, pt: 1, textAlign: "center" }}
  >
    <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
      You&apos;ve reached the end of this selection.
    </Typography>
    <Button
      onClick={onDiscoverNewMix}
      startIcon={<AutoAwesomeIcon />}
      variant="outlined"
      sx={{ textTransform: "none" }}
    >
      Discover a new mix
    </Button>
  </Stack>
);

export default HomeFeedEndState;
