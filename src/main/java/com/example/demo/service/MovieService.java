package com.example.demo.service;

import com.example.demo.entity.Movie;
import com.example.demo.payload.MessageResponse;
import com.example.demo.payload.MovieRecord;
import com.example.demo.payload.MovieRequest;
import com.example.demo.payload.PagedResponse;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size);

  Movie createMovie(MovieRequest movieRequest, UserPrincipal currentAccount);

  Movie updateMovie(Long movieId, MovieRequest request, UserPrincipal currentAccount);

  MessageResponse deleteMovie(Long movieId, UserPrincipal currentAccount);

  List<MovieRecord> searchMoviesByTitle(String title, int page, int size);
}
