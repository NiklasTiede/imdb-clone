import EditNoteIcon from "@mui/icons-material/EditNote";
import Button from "@mui/material/Button";
import { Link as RouterLink } from "react-router";
import { i18n } from "../../../i18n";
import PageContent from "../../../shared/layout/PageContent";
import StatusState from "../../../shared/layout/StatusState";

const EditMoviePage = () => {
  return (
    <PageContent maxWidth="760px">
      <StatusState
        action={
          <Button component={RouterLink} to="/movie-search" variant="contained">
            Search catalog
          </Button>
        }
        icon={<EditNoteIcon />}
        title={i18n.editMovie.heading}
      >
        Admin editing tools will live here. For now this route uses the shared
        app surface so it does not look detached from the rest of the product.
      </StatusState>
    </PageContent>
  );
};

export default EditMoviePage;
