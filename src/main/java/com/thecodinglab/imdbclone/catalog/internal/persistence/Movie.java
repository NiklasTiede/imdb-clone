package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.shared.persistence.DateAudit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
public class Movie extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String imdbId;
  private Long tmdbId;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private MovieType movieType;

  private String primaryTitle;
  private String originalTitle;

  @JsonIgnore private Boolean adult;

  private Integer startYear;

  @JsonIgnore private Integer endYear;

  private Integer runtimeMinutes;

  @Convert(converter = MovieGenreConverterImpl.class)
  private Set<MovieGenre> movieGenre;

  private Float imdbRating;
  private Integer imdbRatingCount;

  @JsonIgnore private String description;

  @Column(length = 255)
  private String posterImageToken;

  @Column(length = 255)
  private String backdropImageToken;

  @Column(length = 255)
  private String trailerYoutubeKey;

  @Column(precision = 3, scale = 1)
  private BigDecimal rating;

  private Integer ratingCount = 0;

  @Column(nullable = false, precision = 19, scale = 1)
  private BigDecimal ratingSum = BigDecimal.ZERO;

  public Movie() {}

  public Movie(
      String primaryTitle, String originalTitle, MovieType movieType, Integer runtimeMinutes) {
    this.primaryTitle = primaryTitle;
    this.originalTitle = originalTitle;
    this.movieType = movieType;
    this.runtimeMinutes = runtimeMinutes;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getImdbId() {
    return imdbId;
  }

  public void setImdbId(String imdbId) {
    this.imdbId = imdbId;
  }

  public Long getTmdbId() {
    return tmdbId;
  }

  public void setTmdbId(Long tmdbId) {
    this.tmdbId = tmdbId;
  }

  public String getPrimaryTitle() {
    return primaryTitle;
  }

  public void setPrimaryTitle(String primaryTitle) {
    this.primaryTitle = primaryTitle;
  }

  public String getOriginalTitle() {
    return originalTitle;
  }

  public void setOriginalTitle(String originalTitle) {
    this.originalTitle = originalTitle;
  }

  public Integer getStartYear() {
    return startYear;
  }

  public void setStartYear(Integer startYear) {
    this.startYear = startYear;
  }

  public Integer getEndYear() {
    return endYear;
  }

  public void setEndYear(Integer endYear) {
    this.endYear = endYear;
  }

  public Integer getRuntimeMinutes() {
    return runtimeMinutes;
  }

  public void setRuntimeMinutes(Integer runtimeMinutes) {
    this.runtimeMinutes = runtimeMinutes;
  }

  public Set<MovieGenre> getMovieGenre() {
    return movieGenre;
  }

  public void setMovieGenre(Set<MovieGenre> movieGenre) {
    this.movieGenre = movieGenre;
  }

  public MovieType getMovieType() {
    return movieType;
  }

  public void setMovieType(MovieType movieType) {
    this.movieType = movieType;
  }

  public Float getImdbRating() {
    return imdbRating;
  }

  public void setImdbRating(Float imdbRating) {
    this.imdbRating = imdbRating;
  }

  public Integer getImdbRatingCount() {
    return imdbRatingCount;
  }

  public void setImdbRatingCount(Integer imdbRatingCount) {
    this.imdbRatingCount = imdbRatingCount;
  }

  public Boolean getAdult() {
    return adult;
  }

  public void setAdult(Boolean adult) {
    this.adult = adult;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public void setRating(BigDecimal rating) {
    this.rating = rating;
  }

  public Integer getRatingCount() {
    return ratingCount;
  }

  public void setRatingCount(Integer ratingCount) {
    this.ratingCount = ratingCount;
  }

  public BigDecimal getRatingSum() {
    return ratingSum;
  }

  public void setRatingSum(BigDecimal ratingSum) {
    this.ratingSum = ratingSum;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPosterImageToken() {
    return posterImageToken;
  }

  public void setPosterImageToken(String posterImageToken) {
    this.posterImageToken = posterImageToken;
  }

  public String getBackdropImageToken() {
    return backdropImageToken;
  }

  public void setBackdropImageToken(String backdropImageToken) {
    this.backdropImageToken = backdropImageToken;
  }

  public String getTrailerYoutubeKey() {
    return trailerYoutubeKey;
  }

  public void setTrailerYoutubeKey(String trailerYoutubeKey) {
    this.trailerYoutubeKey = trailerYoutubeKey;
  }
}
