import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

type SynopsisProps = {
  text?: string | null | undefined;
};

const Synopsis = ({ text }: SynopsisProps) => {
  const synopsis = text?.trim() || "No synopsis available.";

  return (
    <Box
      component="section"
      data-testid="movie-synopsis"
      aria-labelledby="movie-synopsis-title"
      sx={{
        borderTop: "1px solid",
        borderColor: "divider",
        py: { xs: 3, md: 4 },
      }}
    >
      <Box sx={{ width: "100%" }}>
        <Typography
          id="movie-synopsis-title"
          component="h2"
          sx={{ fontSize: 18, mb: 1.25, fontWeight: 700 }}
        >
          Synopsis
        </Typography>
        <Typography
          sx={{
            color: "text.secondary",
            fontSize: { xs: 14, sm: 15 },
            lineHeight: 1.7,
          }}
        >
          {synopsis}
        </Typography>
      </Box>
    </Box>
  );
};

export default Synopsis;
