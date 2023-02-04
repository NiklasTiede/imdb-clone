import { Container, Paper } from "@mui/material";
import { RoleNameEnum } from "../../client/movies/generator-output";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import React from "react";

const AccessDenied = ({ role }: { role: RoleNameEnum }) => {
  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 6, marginTop: 10 }}>
            <Typography
              variant={"inherit"}
              textAlign={"center"}
              sx={{ fontSize: 20, fontWeight: "bold", marginBottom: 3 }}
            >
              {i18n.accessDenied.warning}
            </Typography>
            <Typography
              variant={"inherit"}
              textAlign={"center"}
              sx={{ fontSize: 16 }}
            >
              {i18n.accessDenied.message(role)}
            </Typography>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default AccessDenied;
