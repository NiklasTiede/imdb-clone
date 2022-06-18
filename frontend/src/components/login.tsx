import {makeStyles} from "@material-ui/core";
import React from 'react';

const useStyles = makeStyles({
    login: {
        margin: '30px',
    }
})

export enum LoanType {
    CONSUMER_LOAN,
    CONSTRUCTION_LOAN,
    LEASING
}

export default function Login() {
    const classes = useStyles()

    console.log(LoanType.CONSTRUCTION_LOAN.toString() === 'CONSTRUCTION_LOAN');

    return (
        <div className={classes.login}>
            <h1>Login</h1>
        </div>
    );
}
