import CasinoIcon from "@mui/icons-material/CasinoSharp";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

type WatchlistHeaderProps = {
  disabled: boolean;
  onPickForMe: () => void;
};

const WatchlistHeader = ({ disabled, onPickForMe }: WatchlistHeaderProps) => (
  <Stack
    direction={{ xs: "column", sm: "row" }}
    spacing={1.5}
    sx={{ alignItems: { xs: "stretch", sm: "flex-end" } }}
  >
    <Stack sx={{ flex: 1, minWidth: 0 }}>
      <Typography component="h1" sx={{ fontSize: 24, fontWeight: 600 }}>
        Your watchlist
      </Typography>
      <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
        Movies you've saved to watch later
      </Typography>
    </Stack>
    <Button
      disabled={disabled}
      onClick={onPickForMe}
      startIcon={<CasinoIcon />}
      variant="contained"
    >
      Pick for me
    </Button>
  </Stack>
);

export default WatchlistHeader;
