package app.popcornsociety.catalog.internal.mapper;

import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.catalog.api.MovieRequest;
import app.popcornsociety.catalog.internal.persistence.Movie;
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
