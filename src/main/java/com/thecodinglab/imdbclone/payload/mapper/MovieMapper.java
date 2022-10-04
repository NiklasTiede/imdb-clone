package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.MovieRecord;
import com.thecodinglab.imdbclone.payload.MovieRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovieMapper {

  MovieRecord entityToDTO(Movie movie);

  List<MovieRecord> entityToDTO(Iterable<Movie> movies);

  @Mapping(target = "id", ignore = true)
  Movie dtoToEntity(MovieRecord movieRecord);

  List<Movie> dtoToEntity(Iterable<MovieRecord> movieRecords);

  Movie dtoToEntity(MovieRequest movieRequest);
}
