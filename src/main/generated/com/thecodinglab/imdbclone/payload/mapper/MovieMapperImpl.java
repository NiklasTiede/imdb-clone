package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-10-18T14:12:00+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class MovieMapperImpl implements MovieMapper {

    @Override
    public MovieRecord entityToDTO(Movie movie) {
        if ( movie == null ) {
            return null;
        }

        String primaryTitle = null;
        String originalTitle = null;
        Integer startYear = null;
        Integer endYear = null;
        Integer runtimeMinutes = null;
        Instant modifiedAtInUtc = null;
        Instant createdAtInUtc = null;
        Set<MovieGenreEnum> movieGenre = null;
        MovieTypeEnum movieType = null;
        Float imdbRating = null;
        Integer imdbRatingCount = null;
        Boolean adult = null;
        Float rating = null;
        Integer ratingCount = null;
        String description = null;
        String imageUrlToken = null;

        primaryTitle = movie.getPrimaryTitle();
        originalTitle = movie.getOriginalTitle();
        startYear = movie.getStartYear();
        endYear = movie.getEndYear();
        runtimeMinutes = movie.getRuntimeMinutes();
        modifiedAtInUtc = movie.getModifiedAtInUtc();
        createdAtInUtc = movie.getCreatedAtInUtc();
        Set<MovieGenreEnum> set = movie.getMovieGenre();
        if ( set != null ) {
            movieGenre = new LinkedHashSet<MovieGenreEnum>( set );
        }
        movieType = movie.getMovieType();
        imdbRating = movie.getImdbRating();
        imdbRatingCount = movie.getImdbRatingCount();
        adult = movie.getAdult();
        if ( movie.getRating() != null ) {
            rating = movie.getRating().floatValue();
        }
        ratingCount = movie.getRatingCount();
        description = movie.getDescription();
        imageUrlToken = movie.getImageUrlToken();

        MovieRecord movieRecord = new MovieRecord( primaryTitle, originalTitle, startYear, endYear, runtimeMinutes, modifiedAtInUtc, createdAtInUtc, movieGenre, movieType, imdbRating, imdbRatingCount, adult, rating, ratingCount, description, imageUrlToken );

        return movieRecord;
    }

    @Override
    public List<MovieRecord> entityToDTO(Iterable<Movie> movies) {
        if ( movies == null ) {
            return null;
        }

        List<MovieRecord> list = new ArrayList<MovieRecord>();
        for ( Movie movie : movies ) {
            list.add( entityToDTO( movie ) );
        }

        return list;
    }

    @Override
    public Movie dtoToEntity(MovieRecord movieRecord) {
        if ( movieRecord == null ) {
            return null;
        }

        Movie movie = new Movie();

        movie.setPrimaryTitle( movieRecord.primaryTitle() );
        movie.setOriginalTitle( movieRecord.originalTitle() );
        movie.setStartYear( movieRecord.startYear() );
        movie.setEndYear( movieRecord.endYear() );
        movie.setRuntimeMinutes( movieRecord.runtimeMinutes() );
        Set<MovieGenreEnum> set = movieRecord.movieGenre();
        if ( set != null ) {
            movie.setMovieGenre( new LinkedHashSet<MovieGenreEnum>( set ) );
        }
        movie.setMovieType( movieRecord.movieType() );
        movie.setImdbRating( movieRecord.imdbRating() );
        movie.setImdbRatingCount( movieRecord.imdbRatingCount() );
        movie.setAdult( movieRecord.adult() );
        if ( movieRecord.rating() != null ) {
            movie.setRating( BigDecimal.valueOf( movieRecord.rating() ) );
        }
        movie.setRatingCount( movieRecord.ratingCount() );
        movie.setDescription( movieRecord.description() );
        movie.setImageUrlToken( movieRecord.imageUrlToken() );

        return movie;
    }

    @Override
    public List<Movie> dtoToEntity(Iterable<MovieRecord> movieRecords) {
        if ( movieRecords == null ) {
            return null;
        }

        List<Movie> list = new ArrayList<Movie>();
        for ( MovieRecord movieRecord : movieRecords ) {
            list.add( dtoToEntity( movieRecord ) );
        }

        return list;
    }

    @Override
    public Movie dtoToEntity(MovieRequest movieRequest) {
        if ( movieRequest == null ) {
            return null;
        }

        Movie movie = new Movie();

        movie.setPrimaryTitle( movieRequest.primaryTitle() );
        movie.setOriginalTitle( movieRequest.originalTitle() );
        movie.setStartYear( movieRequest.startYear() );
        movie.setEndYear( movieRequest.endYear() );
        movie.setRuntimeMinutes( movieRequest.runtimeMinutes() );
        Set<MovieGenreEnum> set = movieRequest.movieGenre();
        if ( set != null ) {
            movie.setMovieGenre( new LinkedHashSet<MovieGenreEnum>( set ) );
        }
        movie.setMovieType( movieRequest.movieType() );
        movie.setAdult( movieRequest.adult() );

        return movie;
    }
}
