import {makeStyles, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@material-ui/core";
import React, {useEffect, useState} from "react";


const useStyles = makeStyles({
    table: {
        margin: '10px',
        width: '500px',
    },
    heading: {
        fontWeight: "bolder",
        fontSize: '18px',
    },
    text: {
        fontSize: '16px',
    },
    movieInfo: {
        margin: '10px'
    }
})

const allMoviesUrl = "http://localhost:8080/movies";

export default function MovieList() {
    const [userData, setUserData] = useState([]);
    const classes = useStyles()

    useEffect(() => {
        fetch(allMoviesUrl, {
            method: 'GET',
            headers: {"Content-type": "application/json"}
        })
            .then((response) => (response.json()))
            .then((data) => setUserData(data))
    }, [])

    if (userData.length > 20) {
        console.log("too big!");
        setUserData(userData.slice(0, 15));
    }

    const handleDelete = (movie: any): any => {
        const movies = userData.filter(m => m['id'] !== movie.id );
        setUserData(movies);
    };

    if (userData.length === 0) {
        return ( <TableContainer component={Paper}> <p className={classes.movieInfo}>There are no movies in the db!</p> </TableContainer>)
    }

    return (
        <TableContainer component={Paper}>
            <p className={classes.movieInfo}>Showing { userData.length } movies in the database.</p>
            <Table className={classes.table} aria-label="simple table">
                <TableHead>
                    <TableRow className={classes.heading}>
                        <TableCell className={classes.heading} align="center">Title</TableCell>
                        <TableCell className={classes.heading} align="center">Year</TableCell>
                        <TableCell className={classes.heading} align="center">ID</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {userData.map(movie => (
                        <TableRow className={classes.text} key={movie['id']}>
                            <TableCell className={classes.text} align="center" component="th" scope="row">{movie['title']}</TableCell>
                            <TableCell className={classes.text} align="center">{movie['year']}</TableCell>
                            <TableCell className={classes.text} align="center">{movie['id']}</TableCell>
                            <TableCell className={classes.text} align="center"><button onClick={() => handleDelete(movie)} className="btn btn-danger btn-sm">Delete</button></TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );

}
