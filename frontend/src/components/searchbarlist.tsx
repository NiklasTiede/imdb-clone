import {makeStyles} from "@material-ui/core";
import React, {useEffect, useState} from "react";


const useStyles = makeStyles({
    login: {
        margin: '30px',
    }
})

const allMoviesUrl = "http://localhost:8080/movies";

export default function SearchBarList() {
    const [userData, setUserData] = useState([]);
    const classes = useStyles()

    // useEffect(() => {
    //     fetch(allMoviesUrl, {
    //         method: 'GET',
    //         headers: {"Content-type": "application/json"}
    //     })
    //         .then((response) => (response.json()))
    //         .then((data) => setUserData(data))
    // }, [])


    return (
      <div className={classes.login}>
          <h1>Search Bar and Movies List</h1>

          <input type={"text"} placeholder={"Search..."} />

      </div>
    );

    // return (
    //     <div className={classes.login}>
    //         <h1>Search Bar and Movies List</h1>
    //
    //         <input type={"text"} placeholder={"Search..."} />
    //
    //         {userData.map((movie) => { return <div>{movie['title']}</div>})}
    //
    //     </div>
    // );
}
