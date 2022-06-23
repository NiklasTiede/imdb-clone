package com.example.demo.controller;

import com.example.demo.dto.MovieDto;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.MovieRepository;
import com.example.demo.service.MovieService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MovieController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MovieController.class);
  private final MovieRepository movieRepository;
  private final MovieService movieService;

  public MovieController(MovieRepository movieRepository, MovieService movieService) {
    this.movieRepository = movieRepository;
    this.movieService = movieService;
  }

  @GetMapping("/movies")
  public ResponseEntity<List<MovieDto>> findAllMovies() {
    try {
      List<MovieDto> moviesResponse = movieService.getMovies();
      return new ResponseEntity<>(moviesResponse, HttpStatus.OK);
    } catch (Exception e) {
      LOGGER.error("Movies could not be retrieved from DB");
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/movie/{movieId}")
  public ResponseEntity<MovieDto> findMovieById(@PathVariable Integer movieId) {
    try {
      MovieDto movieResponse = movieService.findMovieById(movieId);
      return new ResponseEntity<>(movieResponse, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/movieExactSearch")
  public ResponseEntity<Movie> findMovieByTitleExactSearch(
      @RequestParam(name = "title", required = false) String title) {
    try {
      Movie movie = movieRepository.findByTitle(title.toLowerCase());
      return new ResponseEntity<>(movie, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/movieSubstringSearch")
  public ResponseEntity<List<Movie>> findMovieByTitleSubstringSearch(
      @RequestParam(name = "title", required = false) String title) {
    try {
      List<Movie> movies = movieRepository.findUsersByKeyword(title);
      return new ResponseEntity<>(movies, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // PUT
//      @PutMapping("/movie/{movieId}")
//      public ResponseEntity<String> updateMovieData(@PathVariable Movie movie) {
//
//          movie.update(movie.getTitle(), movie.getYear());
//          movieRepository.save(movie);
//
//          return movieRepository.find
//
//          return articleRepository
//                  .findBySlug(slug)
//                  .map(
//                          movie -> {
//                              articleCommandService.updateArticle(movie, updateArticleParam);
//                              return ResponseEntity.ok(
//                                      articleResponse(articleQueryService.findBySlug(slug,
//   user).get()));
//                          })
//                  .orElseThrow(ResourceNotFoundException::new);
//
//
//          try {
//              Movie movie =  movieRepository
//                      .findById(movieId)
//                      .orElseThrow(() -> new NotFoundException("Movie with MovieId [" + movieId
//   +
//   "] not found in database."));
//              System.out.println(movie);
//              movieRepository.delete(movie);
//
//              // unpack
//
//              movieRepository.save(movie);
//              return new ResponseEntity<>("the movie '" + movie.getTitle() + "' was deleted
//   successfully.", HttpStatus.OK);
//          } catch (Exception e) {
//              return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//          }
//      }

  // POST

  // DELETE movie by ID
  @DeleteMapping("/movie/{movieId}")
  public ResponseEntity<String> deleteMovieById(@PathVariable int movieId) {
    try {
      Movie movie =
          movieRepository
              .findById(movieId)
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Movie with MovieId [" + movieId + "] not found in database."));
      System.out.println(movie);
      movieRepository.delete(movie);
      return new ResponseEntity<>(
          "the movie '" + movie.getTitle() + "' was deleted successfully.", HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
