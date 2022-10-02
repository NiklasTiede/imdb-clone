package com.example.demo.service;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.MovieRecord;
import com.example.demo.Payload.MovieRequest;
import com.example.demo.entity.Movie;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  List<MovieRecord> findMoviesByIds(List<Long> movieIds);

  Movie createMovie(MovieRequest movieRequest, UserPrincipal currentAccount);

  Movie updateMovie(Long movieId, MovieRequest request, UserPrincipal currentAccount);

  MessageResponse deleteMovie(Long movieId, UserPrincipal currentAccount);

  List<MovieRecord> searchMoviesByTitle(String title);
}
