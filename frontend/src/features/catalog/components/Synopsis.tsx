import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

type SynopsisProps = {
  text?: string | null;
};

const Synopsis = ({ text }: SynopsisProps) => {
  const synopsis = text?.trim() || "No synopsis available.";

  return (
    <Box component="section" sx={{ px: { xs: 2, sm: 3 }, py: 2.5 }}>
      <Typography component="h2" variant="h6" sx={{ mb: 1, fontWeight: 600 }}>
        Synopsis
      </Typography>
      <Typography
        sx={{
          color: "text.secondary",
          fontSize: 14,
          lineHeight: 1.6,
        }}
      >
        {synopsis}
      </Typography>
    </Box>
  );
};

export default Synopsis;
