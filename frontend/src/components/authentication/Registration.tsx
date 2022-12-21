import {Button, Container, Grid, Paper, TextField, useTheme} from "@mui/material";
import React from "react";
import {tokens} from "../../theme";
import {useNavigate} from "react-router-dom";
import {useDispatch} from "react-redux";
import {Dispatch} from "../../redux/store";

const Registration = () => {
    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    const navigateTo = useNavigate();
    const dispatch = useDispatch<Dispatch>()

    return (
        <>
            <div>
                <Container maxWidth={"sm"}>
                    <Grid
                        container
                        spacing={2}
                        direction={"column"}
                        justifyContent={"center"}
                        style={{minHeight: "80vh"}}
                    >
                        <Paper elevation={2} sx={{padding: 5}}>
                            <Grid container direction={"column"} spacing={2}>
                                <Grid item>
                                    <TextField type={"text"}
                                               fullWidth
                                               label={"Enter your Username"}
                                               placeholder={"Username"}
                                               variant={"outlined"}
                                    />
                                </Grid>
                                <Grid item>
                                    <TextField type={"email"}
                                               fullWidth
                                               label={"Enter your Email"}
                                               placeholder={"Email"}
                                               variant={"outlined"}
                                    />
                                </Grid>
                                <Grid item>
                                    <TextField type={"password"}
                                               fullWidth
                                               label={"Enter your Password"}
                                               placeholder={"Password"}
                                               variant={"outlined"}
                                    />
                                </Grid>
                                <Grid item textAlign='center'>
                                    <Button
                                        sx={{
                                            color: colors.primary[100],
                                            backgroundColor: colors.blueAccent[700],
                                            ":hover": {
                                                color: colors.primary[100],
                                                backgroundColor: colors.blueAccent[600]
                                            }
                                        }}
                                        variant={"contained"}
                                        type={"submit"}
                                        fullWidth
                                        onClick={() => console.log('you clicked me.')}>
                                        Register
                                    </Button>
                                </Grid>
                            </Grid>
                        </Paper>
                    </Grid>
                </Container>
            </div>
        </>
    );
}

export default Registration;