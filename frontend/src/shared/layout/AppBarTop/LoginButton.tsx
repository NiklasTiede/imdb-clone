import LoginIcon from "@mui/icons-material/Login";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import { Link as RouterLink } from "react-router";

const LoginButton = () => (
  <>
    <Button
      component={RouterLink}
      sx={{
        borderColor: (theme) => theme.alpha(theme.palette.accent.main, 0.5),
        color: "accent.main",
        display: { xs: "none", sm: "inline-flex" },
        fontWeight: 800,
        minHeight: 40,
        whiteSpace: "nowrap",
      }}
      to="/login"
      variant="outlined"
    >
      Sign in
    </Button>
    <Tooltip title="Sign in">
      <IconButton
        component={RouterLink}
        sx={{
          color: "accent.main",
          display: { xs: "inline-flex", sm: "none" },
        }}
        to="/login"
      >
        <LoginIcon />
      </IconButton>
    </Tooltip>
  </>
);

export default LoginButton;
