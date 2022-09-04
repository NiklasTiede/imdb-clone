package com.example.demo.entity;

import com.example.demo.entity.audit.DateAudit;
import com.example.demo.enums.MovieGenreEnum;
import com.example.demo.enums.MovieTypeEnum;
import java.util.*;
import javax.persistence.*;

@Entity
public class Movie extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String primaryTitle;
  private String originalTitle;
  private Integer startYear;
  private Integer endYear;
  private Integer runtimeMinutes;

  @SuppressWarnings("JpaAttributeTypeInspection")
  private Set<MovieGenreEnum> movieGenre;

  private MovieTypeEnum movieType;
  private Float imdbRating;
  private Integer imdbRatingCount;
  private Boolean adult;
  private Float rating;
  private Integer ratingCount;

  @OneToMany(mappedBy = "movie")
  private Collection<Comment> movieComments;

  @OneToMany(mappedBy = "movie")
  private Collection<Watchlist> watchedMovies;

  // movie?
  @OneToMany(mappedBy = "rating")
  private Collection<Rating> ratings;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Set<MovieGenreEnum> getMovieGenre() {
    return movieGenre;
  }

  public void setMovieGenre(Set<MovieGenreEnum> movieGenre) {
    this.movieGenre = movieGenre;
  }

  public MovieTypeEnum getMovieType() {
    return movieType;
  }

  public void setMovieType(MovieTypeEnum movieType) {
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

  public Float getRating() {
    return rating;
  }

  public void setRating(Float rating) {
    this.rating = rating;
  }

  public Integer getRatingCount() {
    return ratingCount;
  }

  public void setRatingCount(Integer ratingCount) {
    this.ratingCount = ratingCount;
  }

  public Collection<Comment> getMovieComments() {
    return movieComments;
  }

  public void setMovieComments(Collection<Comment> movieComments) {
    this.movieComments = movieComments;
  }

  public Collection<Watchlist> getWatchedMovies() {
    return watchedMovies;
  }

  public void setWatchedMovies(Collection<Watchlist> watchedMovies) {
    this.watchedMovies = watchedMovies;
  }

  public Collection<Rating> getRatings() {
    return ratings;
  }

  public void setRatings(Collection<Rating> ratings) {
    this.ratings = ratings;
  }
}
