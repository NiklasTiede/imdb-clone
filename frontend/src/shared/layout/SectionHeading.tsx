import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

type SectionHeadingProps = {
  children: ReactNode;
};

const SectionHeading = ({ children }: SectionHeadingProps) => (
  <Typography component="h1" variant="h5" sx={{ fontWeight: 600 }}>
    {children}
  </Typography>
);

export default SectionHeading;
