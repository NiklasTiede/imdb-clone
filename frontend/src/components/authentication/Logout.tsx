import { useTheme } from "@mui/material";
import { tokens } from "../../theme";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";
import { useEffect } from "react";

const Logout = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  useEffect(() => {
    navigateTo("/home");
  }, []);

  return <div>You're now logged out!</div>;
};

export default Logout;
