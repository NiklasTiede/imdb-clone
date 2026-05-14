import Paper from "@mui/material/Paper";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";
import { movieColors } from "../../theme";

type SurfaceProps = {
  children: ReactNode;
  elevation?: number;
  sx?: SxProps<Theme>;
};

const Surface = ({ children, elevation = 3, sx }: SurfaceProps) => (
  <Paper
    elevation={elevation}
    sx={{
      backgroundColor: movieColors.surface,
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 1,
      ...sx,
    }}
  >
    {children}
  </Paper>
);

export default Surface;
