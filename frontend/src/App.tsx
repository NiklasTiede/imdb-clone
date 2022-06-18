import { Link } from "react-router-dom";
import {makeStyles} from "@material-ui/core";
import { Provider } from 'react-redux';
import {store} from './redux/store';
import React  from 'react';

const useStyles = makeStyles({
    title: {
        fontFamily: "ubuntu",
        fontSize: "26px",
    },
    home: {
        margin: '30px',
    },
    navbar: {
        borderBottom: "solid 1px",
        paddingBottom: "1rem",
        fontFamily: "ubuntu"
    }
})

export default function App() {
    const classes = useStyles()

  return (
    <Provider store={store}>
      <div className={classes.home}>
          <h1  className={classes.title}>IMDB Clone</h1>
          <nav className={classes.navbar}>
              <Link to="/registration">Registration</Link> |{" "}
              <Link to="/login">Login</Link> |{" "}
              <Link to="/userprofile">User Profile</Link> |{" "}
              <Link to="/moviesearch">Movie Search</Link> |{" "}
              <Link to="/navbar">Navbar</Link> |{" "}
              <Link to="/movielist">Movie List</Link> |{" "}
              <Link to="/counter">Counter</Link> |{" "}
              <Link to="/counters">Counters</Link> |{" "}
              <Link to="/greetings">Greetings</Link> |{" "}
              <Link to="/notes">Notes</Link> |{" "}
              <Link to="/moviedetails">Movie Details</Link> |{" "}
              {/*<Link to="/primarysearchappbar">PrimarySearchAppBar</Link> |{" "}*/}
              <Link to="/searchbarlist">SearchBarList</Link>
          </nav>
      </div>
    </Provider>
  );
}
