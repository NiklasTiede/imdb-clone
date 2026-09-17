import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import { appThemes, isAppThemeId, useThemeSelection } from "../../theme";

/** Development-only theme switcher. Rendered only when `import.meta.env.DEV`. */
const ThemeSwitcher = () => {
  const { canSelectTheme, selectTheme, themeId } = useThemeSelection();
  // Automated browsers (Playwright) test the app itself; the floating switcher
  // would cover controls in the bottom-left corner.
  if (!canSelectTheme || navigator.webdriver) return null;

  return (
    <Box
      data-dev-toolbar=""
      sx={{
        bgcolor: "surface.card",
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 1,
        bottom: 12,
        boxShadow: 6,
        left: 12,
        p: 1,
        position: "fixed",
        width: 180,
        zIndex: (theme) => theme.zIndex.tooltip + 1,
      }}
    >
      <TextField
        fullWidth
        label="Theme (dev)"
        onChange={(event) => {
          if (isAppThemeId(event.target.value)) selectTheme(event.target.value);
        }}
        select
        size="small"
        // Native options render above the fixed switcher; a MUI menu would sit behind it.
        slotProps={{ select: { native: true } }}
        value={themeId}
      >
        {appThemes.map((theme) => (
          <option key={theme.id} value={theme.id}>
            {theme.label}
          </option>
        ))}
      </TextField>
    </Box>
  );
};

export default ThemeSwitcher;
