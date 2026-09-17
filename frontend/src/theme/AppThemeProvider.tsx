import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider, type Theme } from "@mui/material/styles";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { createAppTheme } from "./createAppTheme";
import { persistThemeId, resolveThemeId } from "./themeSelection";
import { appThemes, getAppTheme, type AppThemeId } from "./themes";

type ThemeSelection = {
  themeId: AppThemeId;
  /** Whether the theme may be changed in this build. */
  canSelectTheme: boolean;
  selectTheme: (themeId: AppThemeId) => void;
};

const ThemeSelectionContext = createContext<ThemeSelection | null>(null);

const canSelectTheme = import.meta.env.DEV;

const muiThemes = new Map<AppThemeId, Theme>();
const getMuiTheme = (themeId: AppThemeId) => {
  let theme = muiThemes.get(themeId);
  if (!theme) {
    theme = createAppTheme(getAppTheme(themeId));
    muiThemes.set(themeId, theme);
  }
  return theme;
};

const browserStorage = () => {
  try {
    return window.sessionStorage;
  } catch {
    return undefined;
  }
};

export const AppThemeProvider = ({ children }: { children: ReactNode }) => {
  const [themeId, setThemeId] = useState<AppThemeId>(() =>
    resolveThemeId({
      allowOverride: canSelectTheme,
      search: window.location.search,
      storage: browserStorage(),
    }),
  );

  const selectTheme = useCallback((nextThemeId: AppThemeId) => {
    if (!canSelectTheme) return;
    persistThemeId(nextThemeId, browserStorage());
    setThemeId(nextThemeId);
  }, []);

  useEffect(() => {
    const definition = getAppTheme(themeId);
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", definition.browserThemeColor);
    document
      .querySelector('link[rel="icon"][type="image/svg+xml"]')
      ?.setAttribute("href", definition.brand.faviconSrc);
  }, [themeId]);

  const selection = useMemo(
    () => ({ themeId, canSelectTheme, selectTheme }),
    [themeId, selectTheme],
  );

  return (
    <ThemeSelectionContext.Provider value={selection}>
      <ThemeProvider theme={getMuiTheme(themeId)}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeSelectionContext.Provider>
  );
};

export const useThemeSelection = (): ThemeSelection => {
  const selection = useContext(ThemeSelectionContext);
  if (!selection) {
    throw new Error("useThemeSelection must be used inside AppThemeProvider");
  }
  return selection;
};

export { appThemes };
