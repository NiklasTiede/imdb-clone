import LoginIcon from "@mui/icons-material/Login";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import { alpha } from "@mui/material/styles";
import { Link as RouterLink } from "react-router";
import { movieColors } from "../../../theme";

const LoginButton = () => (
  <>
    <Button
      component={RouterLink}
      sx={{
        borderColor: alpha(movieColors.brand, 0.5),
        color: movieColors.brand,
        display: { xs: "none", sm: "inline-flex" },
        fontWeight: 800,
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
          color: movieColors.brand,
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
