import SearchOffIcon from "@mui/icons-material/SearchOff";
import Button from "@mui/material/Button";
import { Link as RouterLink } from "react-router";
import { i18n } from "../../i18n";
import PageContent from "../../shared/layout/PageContent";
import StatusState from "../../shared/layout/StatusState";

const NotFoundPage = () => {
  return (
    <PageContent maxWidth="760px">
      <StatusState
        action={
          <Button component={RouterLink} to="/" variant="contained">
            Go home
          </Button>
        }
        icon={<SearchOffIcon />}
        title="Page not found"
      >
        {i18n.notFound.message}
      </StatusState>
    </PageContent>
  );
};

export default NotFoundPage;
