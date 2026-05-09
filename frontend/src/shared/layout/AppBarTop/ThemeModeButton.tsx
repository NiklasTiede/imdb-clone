import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import Badge from "@mui/material/Badge";
import IconButton from "@mui/material/IconButton";

type ThemeModeButtonProps = {
  mode: "dark" | "light";
  onToggle: () => void;
};

const ThemeModeButton = ({ mode, onToggle }: ThemeModeButtonProps) => (
  <IconButton
    onClick={onToggle}
    size="large"
    aria-label="toggle color mode"
    color="inherit"
  >
    <Badge>
      {mode === "dark" ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
    </Badge>
  </IconButton>
);

export default ThemeModeButton;
