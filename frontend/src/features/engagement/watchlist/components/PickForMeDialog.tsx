import CasinoIcon from "@mui/icons-material/CasinoSharp";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import { WatchedMovieRecord } from "../../../../client/movies/generator-output";
import { MinioImageSize, PosterImage } from "../../../../shared/media";
import { formatMovieMeta } from "../utils/watchlistFormat";

type PickForMeDialogProps = {
  movie: WatchedMovieRecord | null;
  open: boolean;
  onClose: () => void;
  onPickAnother: () => void;
  canPickAnother: boolean;
};

const PickForMeDialog = ({
  canPickAnother,
  movie,
  onClose,
  onPickAnother,
  open,
}: PickForMeDialogProps) => {
  const selectedMovie = movie?.movie;
  const movieId = movie?.movieId ?? selectedMovie?.id;

  return (
    <Dialog fullWidth maxWidth="xs" onClose={onClose} open={open}>
      <DialogTitle>Tonight's pick</DialogTitle>
      <DialogContent>
        {selectedMovie && (
          <Stack spacing={1.5}>
            <PosterImage
              imageUrlToken={selectedMovie.imageUrlToken}
              size={MinioImageSize.Large}
              sx={{
                aspectRatio: "2 / 3",
                borderRadius: 1,
                maxHeight: 420,
                objectFit: "cover",
              }}
            />
            <Typography sx={{ fontSize: 20, fontWeight: 600 }}>
              {selectedMovie.primaryTitle}
            </Typography>
            <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
              {formatMovieMeta(selectedMovie)}
            </Typography>
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button
          disabled={!canPickAnother}
          onClick={onPickAnother}
          startIcon={<CasinoIcon />}
        >
          Pick another
        </Button>
        {movieId !== undefined && (
          <Button
            component={Link}
            onClick={onClose}
            to={`/movie?id=${movieId}`}
            variant="contained"
          >
            Open movie
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default PickForMeDialog;
