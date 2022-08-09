package com.example.demo.dto.mapper;

import com.example.demo.dto.MovieRecord;
import com.example.demo.entity.Movie;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovieMapper {

  MovieRecord entityToDTO(Movie movie);

  List<MovieRecord> entityToDTO(Iterable<Movie> movie);

  @Mapping(target = "id", ignore = true)
  Movie dtoToEntity(MovieRecord movie);

  List<Movie> dtoToEntity(Iterable<MovieRecord> movie);
}
