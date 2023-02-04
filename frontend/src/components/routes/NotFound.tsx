import { Container, Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import React from "react";

const NotFound = () => {
  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 6, marginTop: 10 }}>
            <Typography
              variant={"inherit"}
              textAlign={"center"}
              sx={{ fontSize: 16 }}
            >
              {i18n.notFound.message}
            </Typography>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default NotFound;
