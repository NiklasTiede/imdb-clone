import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

type PageHeaderProps = {
  eyebrow?: ReactNode;
  subtitle?: ReactNode;
  title: ReactNode;
};

const PageHeader = ({ eyebrow, subtitle, title }: PageHeaderProps) => (
  <Box>
    {eyebrow && (
      <Typography
        sx={{
          color: "primary.main",
          fontSize: 11,
          fontWeight: 700,
          mb: 0.5,
          textTransform: "uppercase",
        }}
      >
        {eyebrow}
      </Typography>
    )}
    <Typography component="h1" sx={{ fontSize: 24, fontWeight: 700 }}>
      {title}
    </Typography>
    {subtitle && (
      <Typography sx={{ color: "text.secondary", fontSize: 14, mt: 0.5 }}>
        {subtitle}
      </Typography>
    )}
  </Box>
);

export default PageHeader;
