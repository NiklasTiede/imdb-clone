import EditIcon from "@mui/icons-material/Edit";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import { Link as RouterLink } from "react-router";

const AdminEditButton = () => (
  <Tooltip title="Edit movies">
    <IconButton
      aria-label="edit movies"
      color="inherit"
      component={RouterLink}
      size="large"
      to="/editing"
    >
      <EditIcon />
    </IconButton>
  </Tooltip>
);

export default AdminEditButton;
