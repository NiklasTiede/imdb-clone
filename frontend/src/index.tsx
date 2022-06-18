import React, { render } from "react-dom";
import {
    BrowserRouter,
    Routes,
    Route
} from "react-router-dom";
import App from "./App";
import Login from "./components/login";
import Registration from "./components/registration";
import MovieSearch from "./components/moviesearch";
import UserProfile from "./components/userprofile";
import MovieList from "./components/movielist";
import Navbar from "./components/navbar";
import Counter from "./components/counter";
import Counters from "./components/counters";
import Greetings from "./components/greetings";
import 'bootstrap/dist/css/bootstrap.css';
import './index.css';
import SearchBarList from "./components/searchbarlist";
import Notes from './components/notes';
import {Provider} from 'react-redux';
import {store} from './redux/store';
import MovieDetails from './components/moviedetails';
// import PrimarySearchAppBar from "./components/primarysearchAppbar";

const rootElement = document.getElementById("root");
render(
  <Provider store={store}>
    <BrowserRouter>
        <Routes>
            <Route path="/" element={<App />} />
            <Route path="login" element={<Login />} />
            <Route path="registration" element={<Registration />} />
            <Route path="moviesearch" element={<MovieSearch />} />
            <Route path="userprofile" element={<UserProfile />} />
            <Route path="navbar" element={<Navbar />} />
            <Route path="movielist" element={<MovieList />} />
            <Route path="counter" element={<Counter />} />
            <Route path="counters" element={<Counters />} />
            <Route path="greetings" element={<Greetings />} />
            <Route path="notes" element={<Notes />} />
            <Route path="moviedetails" element={<MovieDetails />} />
            {/*<Route path="primarysearchappbar" element={<PrimarySearchAppBar />} />*/}
            <Route path="searchbarlist" element={<SearchBarList />} />
        </Routes>
    </BrowserRouter></Provider>,
    rootElement
);