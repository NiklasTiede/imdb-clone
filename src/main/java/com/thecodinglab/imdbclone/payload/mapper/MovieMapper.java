package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieMapper {

  MovieRecord entityToDTO(Movie movie);

  List<MovieRecord> entityToDTO(Iterable<Movie> movies);

  @Mapping(target = "id", ignore = true)
  Movie dtoToEntity(MovieRecord movieRecord);

  List<Movie> dtoToEntity(Iterable<MovieRecord> movieRecords);

  Movie dtoToEntity(MovieRequest movieRequest);
}
