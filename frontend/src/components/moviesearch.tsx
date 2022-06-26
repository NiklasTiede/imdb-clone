import {Button, makeStyles, TextField} from "@material-ui/core";
import React, {useState} from 'react';

import SearchIcon from '@mui/icons-material/Search';


const useStyles = makeStyles({
    movieSearch: {
        margin: '30px',
        // backgroundImage: 'linear-gradient(90deg, #14ebff, #11fab4);'
    },
    searchInputs: {
        marginTop: '105px',
        display: 'flex',
        placeContent: 'center'
    },
    searchIcon: {
        marginLeft: '20px',
    },
    dataResult: {},
})




export default function MovieSearch(data: any) {

    const classes = useStyles()




    const [filteredData, setFilteredData] = useState([]);
    const [wordEntered, setWordEntered] = useState("");

    const [query, setQuery] = useState("")

    return (
        <div className={classes.movieSearch}>

            <form action="/moviesearch/" method="get">

                <TextField
                    type="text"
                    id="header-search"
                    placeholder="Search Movies"
                    name="s"
                    onChange={event => console.log(event.target.value)}  //  setQuery(event.target.value)
                />

                <button type="submit">Search</button>

            </form>


            <h1>Movie Search</h1>
            <TextField id="standard-basic" label="Search" variant="standard" onChange={event => console.log(event.target.value)}/>
            <Button variant="text">Go!</Button>


        </div>
    );
}


