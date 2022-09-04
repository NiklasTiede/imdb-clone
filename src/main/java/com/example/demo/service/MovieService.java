package com.example.demo.service;

import com.example.demo.Payload.MovieRecord;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  List<MovieRecord> findMovieByTitle(String title);

  String saveMovie(MovieRecord movieRecord);

  String deleteMovie(Long movieId);
}
