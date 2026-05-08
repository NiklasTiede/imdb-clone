import { Container, Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import React from "react";

const EditMovie = () => {
  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 4, marginTop: 10, fontSize: 18 }}>
            <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
              {i18n.editMovie.heading}
            </Typography>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default EditMovie;
