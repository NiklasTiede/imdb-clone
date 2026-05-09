import EditIcon from "@mui/icons-material/Edit";
import IconButton from "@mui/material/IconButton";
import { Link } from "react-router";

const AdminEditButton = () => (
  <Link style={{ color: "inherit" }} to="editing">
    <IconButton size="large" aria-haspopup="true" color="inherit">
      <EditIcon />
    </IconButton>
  </Link>
);

export default AdminEditButton;
