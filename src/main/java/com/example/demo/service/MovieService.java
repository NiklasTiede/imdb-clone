package com.example.demo.service;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.MovieRecord;
import com.example.demo.Payload.MovieRequest;
import com.example.demo.entity.Movie;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  List<MovieRecord> findMoviesByIds(List<Long> movieIds);

  Movie createMovie(MovieRequest movieRequest);

  Movie updateMovie(Long movieId, MovieRequest request);

  MessageResponse deleteMovie(Long movieId);

  List<MovieRecord> searchMoviesByTitle(String title);
}
