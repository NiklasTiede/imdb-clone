import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import { alpha } from "@mui/material/styles";
import { Button, Fab, useMediaQuery, useTheme } from "@mui/material";
import { useCallback, useState } from "react";
import { useNavigate } from "react-router";
import { useAuthSessionSnapshot } from "../../../shared/auth";
import { movieDetailPath } from "../../../shared/navigation/appRoutes";
import { movieColors } from "../../../theme";
import { getConciergeClientId } from "../model/browserIdentity";
import ConciergeDrawer from "./ConciergeDrawer";
import type { OpenMovieAction } from "../model/concierge";

const ConciergeExperience = () => {
  const { bootstrapped, session } = useAuthSessionSnapshot();
  if (!bootstrapped) {
    return null;
  }
  const identity = session?.id ?? null;
  return (
    <IdentityScopedConcierge
      key={identity ?? "anonymous"}
      accountId={identity}
    />
  );
};

const IdentityScopedConcierge = ({
  accountId,
}: {
  accountId: number | null;
}) => {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const theme = useTheme();
  const mobile = useMediaQuery(theme.breakpoints.down("sm"));
  const clientId = getConciergeClientId(accountId);
  const openMovie = useCallback(
    (action: OpenMovieAction) => {
      const destination = movieDetailPath(action.movieId);
      setOpen(false);
      void navigate(destination);
    },
    [navigate],
  );

  return (
    <>
      {mobile ? (
        <Fab
          aria-label="Ask the Movie Concierge"
          onClick={() => setOpen(true)}
          size="medium"
          sx={{
            bgcolor: movieColors.brand,
            bottom: 18,
            boxShadow: "0 12px 32px rgba(0,0,0,0.42)",
            color: movieColors.brandInk,
            position: "fixed",
            right: 18,
            zIndex: theme.zIndex.fab,
            "&:hover": { bgcolor: "#ffe053" },
          }}
        >
          <AutoAwesomeRoundedIcon />
        </Fab>
      ) : (
        <Button
          aria-label="Ask the Movie Concierge"
          onClick={() => setOpen(true)}
          startIcon={<AutoAwesomeRoundedIcon sx={{ fontSize: 17 }} />}
          sx={{
            backdropFilter: "blur(12px)",
            bgcolor: alpha(movieColors.surfaceElevated, 0.94),
            border: `1px solid ${alpha(movieColors.brand, 0.35)}`,
            borderRadius: 10,
            bottom: 24,
            boxShadow: "0 14px 38px rgba(0,0,0,0.38)",
            color: "rgba(255,255,255,0.9)",
            fontSize: 11.5,
            fontWeight: 700,
            px: 2,
            py: 1.1,
            position: "fixed",
            right: 24,
            textTransform: "none",
            zIndex: theme.zIndex.fab,
            "& .MuiButton-startIcon": { color: movieColors.brand },
            "&:hover": {
              bgcolor: movieColors.surfaceElevated,
              borderColor: alpha(movieColors.brand, 0.68),
            },
          }}
        >
          Ask Concierge
        </Button>
      )}
      <ConciergeDrawer
        clientId={clientId}
        onClose={() => setOpen(false)}
        onUiAction={openMovie}
        open={open}
      />
    </>
  );
};

export default ConciergeExperience;
