import { Container, Paper } from "@mui/material";
import { RoleNameEnum } from "../../shared/auth";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import React from "react";

const AccessDeniedPage = ({ role }: { role: RoleNameEnum }) => {
  return (
    <>
      <div>
        <Container maxWidth={"xs"}>
          <Paper elevation={3} sx={{ padding: 6, marginTop: 10 }}>
            <Typography
              variant={"inherit"}
              sx={{
                textAlign: "center",
                fontSize: 20,
                fontWeight: "bold",
                marginBottom: 3,
              }}
            >
              {i18n.accessDenied.warning}
            </Typography>
            <Typography
              variant={"inherit"}
              sx={{ textAlign: "center", fontSize: 16 }}
            >
              {i18n.accessDenied.message(role)}
            </Typography>
          </Paper>
        </Container>
      </div>
    </>
  );
};

export default AccessDeniedPage;
