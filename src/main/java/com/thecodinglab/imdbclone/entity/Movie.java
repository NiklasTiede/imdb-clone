package com.thecodinglab.imdbclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.entity.audit.DateAudit;
import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import com.thecodinglab.imdbclone.enums.attributeconverter.MovieGenreConverterImpl;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Entity
@Document(indexName = "movies", writeTypeHint = WriteTypeHint.FALSE)
public class Movie extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String primaryTitle;
  private String originalTitle;
  private Integer startYear;

  @JsonIgnore private Integer endYear;

  private Integer runtimeMinutes;

  @Convert(converter = MovieGenreConverterImpl.class)
  private Set<MovieGenreEnum> movieGenre;

  @Enumerated(EnumType.ORDINAL)
  private MovieTypeEnum movieType;

  private Float imdbRating;
  private Integer imdbRatingCount;

  @JsonIgnore private Boolean adult;

  private BigDecimal rating;
  private Integer ratingCount;

  @JsonIgnore private String description;

  private String imageUrlToken;

  @Transient
  @JsonIgnore
  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<Comment> comments;

  @Transient
  @JsonIgnore
  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<WatchedMovie> watchedMovies;

  @Transient
  @JsonIgnore
  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<Rating> ratings;

  public Movie() {}

  public Movie(
      String primaryTitle, String originalTitle, MovieTypeEnum movieType, Integer runtimeMinutes) {
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getImageUrlToken() {
    return imageUrlToken;
  }

  public void setImageUrlToken(String imageUrlToken) {
    this.imageUrlToken = imageUrlToken;
  }

  public Collection<Comment> getComments() {
    return comments;
  }

  public void setComments(Collection<Comment> comments) {
    this.comments = comments;
  }

  public Collection<WatchedMovie> getWatchedMovies() {
    return watchedMovies;
  }

  public void setWatchedMovies(Collection<WatchedMovie> watchedMovies) {
    this.watchedMovies = watchedMovies;
  }

  public Collection<Rating> getRatings() {
    return ratings;
  }

  public void setRatings(Collection<Rating> ratings) {
    this.ratings = ratings;
  }
}
