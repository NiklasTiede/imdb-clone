import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import Button from "@mui/material/Button";
import { Link as RouterLink } from "react-router";
import { RoleNameEnum } from "../../shared/auth";
import { i18n } from "../../i18n";
import PageContent from "../../shared/layout/PageContent";
import StatusState from "../../shared/layout/StatusState";

const AccessDeniedPage = ({ role }: { role: RoleNameEnum }) => {
  return (
    <PageContent maxWidth="760px">
      <StatusState
        action={
          <Button component={RouterLink} to="/" variant="contained">
            Go home
          </Button>
        }
        icon={<LockOutlinedIcon />}
        title={i18n.accessDenied.warning}
      >
        {i18n.accessDenied.message(role)}
      </StatusState>
    </PageContent>
  );
};

export default AccessDeniedPage;
