import StarBorderIcon from "@mui/icons-material/StarBorder";
import Button from "@mui/material/Button";
import { Link as RouterLink } from "react-router";
import StatusState from "../../../../shared/layout/StatusState";

const EmptyRatings = () => (
  <StatusState
    action={
      <Button component={RouterLink} to="/movie-search" variant="contained">
        Browse movies
      </Button>
    }
    icon={<StarBorderIcon />}
    title="No ratings yet"
  >
    Rate movies to start building your taste profile.
  </StatusState>
);

export default EmptyRatings;
