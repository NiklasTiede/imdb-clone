import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined";
import Button from "@mui/material/Button";
import { Link as RouterLink } from "react-router";
import { i18n } from "../../../i18n";
import PageContent from "../../../shared/layout/PageContent";
import StatusState from "../../../shared/layout/StatusState";

const Messages = () => {
  return (
    <PageContent maxWidth="760px">
      <StatusState
        action={
          <Button component={RouterLink} to="/movie-search" variant="contained">
            Browse movies
          </Button>
        }
        icon={<InboxOutlinedIcon />}
        title={i18n.messages.heading}
      >
        Conversations and replies will appear here once the community features
        are active.
      </StatusState>
    </PageContent>
  );
};

export default Messages;
