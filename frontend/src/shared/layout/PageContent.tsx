import Box from "@mui/material/Box";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";

type PageContentProps = {
  children: ReactNode;
  maxWidth?: number | string;
  sx?: SxProps<Theme>;
};

const PageContent = ({
  children,
  maxWidth = "960px",
  sx,
}: PageContentProps) => (
  <Box
    component="main"
    sx={{
      width: "100%",
      maxWidth,
      mx: "auto",
      px: { xs: 2, sm: 3 },
      py: { xs: 2, sm: 3 },
      ...sx,
    }}
  >
    {children}
  </Box>
);

export default PageContent;
