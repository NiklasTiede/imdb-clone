import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import type { ReactNode } from "react";

type MovieListViewProps = {
  ariaLabel: string;
  children: ReactNode;
};

const MovieListView = ({ ariaLabel, children }: MovieListViewProps) => (
  <Box
    sx={{
      borderTop: "1px solid",
      borderColor: "divider",
      alignSelf: "center",
      maxWidth: 980,
      mx: "auto",
      pt: 0.75,
      width: "100%",
    }}
  >
    <Stack
      aria-label={ariaLabel}
      component="ul"
      role="list"
      spacing={0.5}
      sx={{ m: 0, p: 0 }}
    >
      {children}
    </Stack>
  </Box>
);

export default MovieListView;
