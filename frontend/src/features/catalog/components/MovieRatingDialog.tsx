import DeleteIcon from "@mui/icons-material/Delete";
import StarIcon from "@mui/icons-material/Star";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import Rating from "@mui/material/Rating";
import Typography from "@mui/material/Typography";
import { useEffect, useState } from "react";
import { movieColors } from "../../../theme";

type MovieRatingDialogProps = {
  currentRating: number | null;
  errorMessage?: string | null;
  isPending?: boolean;
  movieTitle: string;
  onClose: () => void;
  onSubmit: (score: number | null) => void;
  open: boolean;
};

const MovieRatingDialog = ({
  currentRating,
  errorMessage = null,
  isPending = false,
  movieTitle,
  onClose,
  onSubmit,
  open,
}: MovieRatingDialogProps) => {
  const [selectedRating, setSelectedRating] = useState<number | null>(
    currentRating,
  );

  useEffect(() => {
    if (open) {
      setSelectedRating(currentRating);
    }
  }, [currentRating, open]);

  const handleClose = () => {
    if (!isPending) {
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="movie-rating-dialog-title"
      aria-describedby="movie-rating-dialog-description"
    >
      <DialogTitle id="movie-rating-dialog-title">Rate this movie</DialogTitle>
      <DialogContent>
        <DialogContentText id="movie-rating-dialog-description">
          Choose a score from 1 to 10 for {movieTitle}.
        </DialogContentText>

        {errorMessage && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {errorMessage}
          </Alert>
        )}

        <Box
          sx={{
            alignItems: "center",
            display: "flex",
            flexDirection: "column",
            minHeight: 112,
            pt: 3,
          }}
        >
          <Typography
            aria-live="polite"
            sx={{ fontSize: 28, fontWeight: 700, lineHeight: 1, mb: 1.5 }}
          >
            {selectedRating ?? "–"}
            <Box
              component="span"
              sx={{ color: "text.secondary", fontSize: 14, fontWeight: 500 }}
            >
              {" "}/ 10
            </Box>
          </Typography>
          <Rating
            name="movie-rating"
            max={10}
            value={selectedRating}
            disabled={isPending}
            getLabelText={(value) => `${value} out of 10`}
            onChange={(event, value) => {
              const fallbackValue = Number(
                (event.target as HTMLInputElement).value,
              );
              const nextValue =
                typeof value === "number" && Number.isFinite(value)
                  ? value
                  : fallbackValue;

              if (Number.isFinite(nextValue)) {
                setSelectedRating(nextValue);
              }
            }}
            icon={<StarIcon fontSize="inherit" />}
            emptyIcon={<StarBorderIcon fontSize="inherit" />}
            sx={{
              color: movieColors.rating,
              fontSize: { xs: 25, sm: 29 },
              "& .MuiRating-iconEmpty": { color: "rgba(255,255,255,0.3)" },
            }}
          />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        {currentRating !== null && (
          <Button
            color="error"
            startIcon={<DeleteIcon />}
            disabled={isPending}
            onClick={() => onSubmit(null)}
            sx={{ mr: "auto", textTransform: "none" }}
          >
            Remove
          </Button>
        )}
        <Button
          onClick={handleClose}
          disabled={isPending}
          sx={{ textTransform: "none" }}
        >
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={selectedRating === null || isPending}
          onClick={() => onSubmit(selectedRating)}
          startIcon={
            isPending ? <CircularProgress color="inherit" size={16} /> : null
          }
          sx={{ textTransform: "none" }}
        >
          {isPending ? "Saving..." : "Save rating"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default MovieRatingDialog;
