import {Button, makeStyles, TextField} from "@material-ui/core";
import React from 'react';

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

    // Should I create a page (MovieSearchPage) component and add each component into it?
    // searchbar with proposals
    // list of movies


    const classes = useStyles()

    return (
        <div className={classes.movieSearch}>

                <div className={classes.searchInputs}>

                    <input type={"text"} placeholder={"Some text"} />

                    <div className={classes.searchIcon}>

                        <SearchIcon />

                    </div>

                </div>


                <div className={classes.dataResult}>

                    {/*{data.map((value: string, key: string) => {*/}
                    {/*    */}
                    {/*})}*/}

                </div>


            {/*<h1>Movie Search</h1>*/}
            {/*<TextField id="standard-basic" label="Search" variant="standard" />*/}
            {/*<Button variant="text">Go!</Button>*/}


        </div>
    );
}


