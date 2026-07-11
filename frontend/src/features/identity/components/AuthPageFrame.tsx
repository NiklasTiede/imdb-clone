import Box from "@mui/material/Box";
import type { ReactNode } from "react";
import AuthVisualPane from "./AuthVisualPane";

type AuthPageFrameProps = {
  children: ReactNode;
  formMaxWidth?: number;
  variant: "login" | "signup";
};

const AuthPageFrame = ({
  children,
  formMaxWidth = 440,
  variant,
}: AuthPageFrameProps) => (
  <Box
    sx={{
      display: "grid",
      gridTemplateColumns: { xs: "minmax(0, 1fr)", md: "1fr 1fr" },
      height: { md: "100%" },
      minHeight: "100%",
    }}
  >
    <AuthVisualPane variant={variant} />
    <Box
      sx={{
        alignItems: { xs: "flex-start", md: "center" },
        display: "flex",
        justifyContent: "center",
        minHeight: 0,
        overflowY: { md: "auto" },
        px: { xs: 2.5, sm: 5, md: 7 },
        py: { xs: 4, md: 6 },
        "@media (min-width: 900px) and (max-height: 780px)": {
          alignItems: "flex-start",
        },
      }}
    >
      <Box sx={{ maxWidth: formMaxWidth, width: "100%" }}>{children}</Box>
    </Box>
  </Box>
);

export default AuthPageFrame;
