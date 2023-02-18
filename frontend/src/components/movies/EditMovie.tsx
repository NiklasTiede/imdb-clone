import { Button } from "@mui/material";
import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";
import { MovieSearchRequestMovieTypeEnum } from "../../client/movies/generator-output";

const EditMovie = () => {
  const dispatch = useDispatch<Dispatch>();

  function handleClick() {
    dispatch.notify.success("This is a success message!");

    let payload: any = {
        query: "it",
        requestSearchParams: {
            minRuntimeMinutes: 80,
            maxRuntimeMinutes: 230,
            minStartYear: 2010,
            maxStartYear: 2022,
            movieGenre: ["HORROR"],
            movieType: MovieSearchRequestMovieTypeEnum.Movie,
            adult: false,
        },
        page: 0,
        size: 20
    };
    dispatch.search.searchMovies(payload);
  }

  function handleClick2() {
    dispatch.notify.info("This is an info message!");
  }

  function handleClick3() {
    dispatch.notify.warn("This is a warning message!");
  }

  function handleClick4() {
    dispatch.notify.error("This is an error message!");
  }

  return (
    <div>
      Edit Movie
      <Button onClick={() => handleClick()} variant="contained">
        success
      </Button>
      <Button onClick={() => handleClick2()} variant="contained">
        info
      </Button>
      <Button onClick={() => handleClick3()} variant="contained">
        warn
      </Button>
      <Button onClick={() => handleClick4()} variant="contained">
        error
      </Button>
    </div>
  );
};

export default EditMovie;
