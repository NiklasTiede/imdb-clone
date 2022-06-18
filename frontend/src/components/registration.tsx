import React, { useState } from 'react';
import {Button, TextField, Typography} from "@material-ui/core";
import {Container} from "@material-ui/core";
import { makeStyles } from "@material-ui/core";
import { useNavigate } from 'react-router-dom';

const useStyles = makeStyles({
    btn: {
        fontSize: 15,
        backgroundColor: 'darkblue',
        margin: '10px',
        '&:hover': {
            backgroundColor: 'blue'
        }
    },
    title: {
        margin: '10px',
        color:'black',
    },
    field: {
        margin: '10px',
    },
    form: {
        margin: '20px',

    }
})


export default function Create() {
    const navigateTo = useNavigate();
    const classes = useStyles()
    const [username, setUsername] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    const [usernameError, setUsernameError] = useState(false)
    const [emailError, setEmailError] = useState(false)
    const [passwordError, setPasswordError] = useState(false)


    const handleSubmit = (e: any) => {
        e.preventDefault()
        setEmailError(false);
        setUsernameError(false);
        setPasswordError(false);
        if (username === '') {
            setUsernameError(true);
        }
        if (email === '') {
            setEmailError(true);
        }
        if (password === '') {
            setPasswordError(true);
        }
        if (email && username && password) {
            fetch("http://localhost:8080/register", {
                method: 'POST',
                headers: {"Content-type": "application/json"},
                body: JSON.stringify({ username, email, password })
            })
                .then(() => console.log("POST was send!"))
        }
        navigateTo("/")
    }

    return (
        <Container className={classes.form} >
            <Typography
                className={classes.title}
                variant="h4"
                component={"h1"}
                color={"secondary"}
                align={"center"}>
                Registration
            </Typography>
            <form noValidate autoCorrect={"off"} autoComplete={"on"} onSubmit={handleSubmit}>
                <TextField
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    className={classes.field}
                    id="outlined-basic"
                    label="Username"
                    variant="outlined"
                    error={usernameError}
                />
                <TextField
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className={classes.field}
                    id="outlined-basic"
                    label="Email"
                    variant="outlined"
                    error={emailError}
                />
                <TextField
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className={classes.field}
                    id="outlined-basic"
                    label="Password"
                    variant="outlined"
                    error={passwordError}
                />
                <br/>
                <Button
                    className={classes.btn}
                    color={"primary"}
                    variant={"contained"}
                    type={"submit"}
                    onClick={() => console.log('you clicked me.')}>
                    Submit
                </Button>
            </form>
        </Container>
    );
}
