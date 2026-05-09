import LoginIcon from "@mui/icons-material/Login";
import Badge from "@mui/material/Badge";
import IconButton from "@mui/material/IconButton";
import { Link } from "react-router";

const LoginButton = () => (
  <Link style={{ color: "inherit" }} to="login">
    <IconButton size="large" color="inherit">
      <Badge>
        <LoginIcon />
      </Badge>
    </IconButton>
  </Link>
);

export default LoginButton;
