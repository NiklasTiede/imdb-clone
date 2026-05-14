import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";
import { movieColors } from "../../theme";
import AppSurface from "./AppSurface";

type StatusStateProps = {
  action?: ReactNode;
  children: ReactNode;
  icon?: ReactNode;
  title: ReactNode;
};

const StatusState = ({ action, children, icon, title }: StatusStateProps) => (
  <AppSurface
    sx={{
      alignItems: "center",
      display: "flex",
      justifyContent: "center",
      minHeight: { xs: 240, sm: 300 },
      p: { xs: 3, sm: 5 },
      textAlign: "center",
    }}
  >
    <Stack spacing={1.5} sx={{ alignItems: "center", maxWidth: 440 }}>
      {icon && (
        <Stack
          aria-hidden
          sx={{
            alignItems: "center",
            backgroundColor: "rgba(77,171,247,0.12)",
            border: "1px solid rgba(77,171,247,0.22)",
            borderRadius: "50%",
            color: movieColors.info,
            height: 56,
            justifyContent: "center",
            width: 56,
            "& svg": { fontSize: 30 },
          }}
        >
          {icon}
        </Stack>
      )}
      <Typography component="h2" sx={{ fontSize: 20, fontWeight: 700 }}>
        {title}
      </Typography>
      <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
        {children}
      </Typography>
      {action && <Stack sx={{ pt: 0.5 }}>{action}</Stack>}
    </Stack>
  </AppSurface>
);

export default StatusState;
