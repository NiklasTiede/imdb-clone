import { Button, useTheme } from "@mui/material";
import { tokens } from "../../theme";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";
// import {useNotifier} from "../../hooks/useNotifier";

const EditMovie = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  // useNotifier();

  function handleClick() {
    dispatch.notify.success("This is a success message!");
  }

  function handleClick2() {
    dispatch.notify.info("This is an info message!");
  }

  function handleClick3() {
    dispatch.notify.warn("This is a warning message!");
  }

  function handleClick4() {
    dispatch.notify.error("This is an error message!");
  }

  return (
    <div>
      Edit Movie
      <Button onClick={() => handleClick()} variant="contained">
        success
      </Button>
      <Button onClick={() => handleClick2()} variant="contained">
        info
      </Button>
      <Button onClick={() => handleClick3()} variant="contained">
        warn
      </Button>
      <Button onClick={() => handleClick4()} variant="contained">
        error
      </Button>
    </div>
  );
};

export default EditMovie;
