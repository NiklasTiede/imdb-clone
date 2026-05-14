import Paper from "@mui/material/Paper";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";
import { movieColors } from "../../theme";

type SurfaceAccent = "brand" | "info" | "none";

type AppSurfaceProps = {
  accent?: SurfaceAccent;
  children: ReactNode;
  sx?: SxProps<Theme>;
};

const accentColors: Record<Exclude<SurfaceAccent, "none">, string> = {
  brand: movieColors.brand,
  info: movieColors.info,
};

const AppSurface = ({
  accent = "none",
  children,
  sx,
}: AppSurfaceProps) => (
  <Paper
    elevation={0}
    sx={{
      backgroundColor: movieColors.surface,
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 1,
      overflow: "hidden",
      position: "relative",
      ...(accent !== "none" && {
        "&::before": {
          backgroundColor: accentColors[accent],
          content: '""',
          height: 2,
          inset: "0 0 auto 0",
          position: "absolute",
        },
      }),
      ...sx,
    }}
  >
    {children}
  </Paper>
);

export default AppSurface;
