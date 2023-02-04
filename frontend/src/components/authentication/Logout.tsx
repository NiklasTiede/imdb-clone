import { Container, Paper } from "@mui/material";
import { useNavigate } from "react-router-dom";
import React, { useEffect } from "react";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";

const Logout = () => {
  const navigateTo = useNavigate();

  useEffect(() => {
    navigateTo("/logout");
  }, [navigateTo]);

  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 4, marginTop: 10, fontSize: 18 }}>
            <Typography variant={"inherit"} textAlign={"center"}>
              {i18n.logout.message}
            </Typography>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default Logout;
