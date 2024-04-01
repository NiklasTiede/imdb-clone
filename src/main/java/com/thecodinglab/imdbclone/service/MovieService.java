package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  Page<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size);

  MovieRecord createMovie(MovieRequest movieRequest, UserPrincipal currentAccount);

  MovieRecord updateMovie(Long movieId, MovieRequest request, UserPrincipal currentAccount);

  MessageResponse deleteMovie(Long movieId, UserPrincipal currentAccount);

  PagedResponse<Movie> searchMoviesByTitle(String title, int page, int size);

  Movie performSave(Movie movie);
}
