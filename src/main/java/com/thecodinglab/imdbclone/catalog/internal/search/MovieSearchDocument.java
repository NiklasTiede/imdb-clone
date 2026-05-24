package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Document(indexName = "movies", createIndex = false, writeTypeHint = WriteTypeHint.FALSE)
@Setting(shards = 1, replicas = 0)
public class MovieSearchDocument {

  @Id private Long id;

  private String imdbId;
  private Long tmdbId;
  private MovieType movieType;
  private String primaryTitle;
  private String originalTitle;
  private Boolean adult;
  private Integer startYear;
  private Integer endYear;
  private Integer runtimeMinutes;
  private Instant modifiedAtInUtc;
  private Instant createdAtInUtc;
  private Set<MovieGenre> movieGenre;
  private Float imdbRating;
  private Integer imdbRatingCount;
  private String description;
  private String posterImageToken;
  private String backdropImageToken;
  private String trailerYoutubeKey;
  private Float rating;
  private Integer ratingCount;
  private String imageUrlToken;
  private String embeddingModel;
  private String embeddingTextVersion;

  @Field(
      type = FieldType.Dense_Vector,
      dims = 768,
      index = true,
      knnSimilarity = KnnSimilarity.COSINE)
  private float[] embedding;

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

  public MovieType getMovieType() {
    return movieType;
  }

  public void setMovieType(MovieType movieType) {
    this.movieType = movieType;
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

  public Boolean getAdult() {
    return adult;
  }

  public void setAdult(Boolean adult) {
    this.adult = adult;
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

  public Instant getModifiedAtInUtc() {
    return modifiedAtInUtc;
  }

  public void setModifiedAtInUtc(Instant modifiedAtInUtc) {
    this.modifiedAtInUtc = modifiedAtInUtc;
  }

  public Instant getCreatedAtInUtc() {
    return createdAtInUtc;
  }

  public void setCreatedAtInUtc(Instant createdAtInUtc) {
    this.createdAtInUtc = createdAtInUtc;
  }

  public Set<MovieGenre> getMovieGenre() {
    return movieGenre;
  }

  public void setMovieGenre(Set<MovieGenre> movieGenre) {
    this.movieGenre = movieGenre;
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

  public String getImageUrlToken() {
    return imageUrlToken;
  }

  public void setImageUrlToken(String imageUrlToken) {
    this.imageUrlToken = imageUrlToken;
  }

  public String getEmbeddingModel() {
    return embeddingModel;
  }

  public void setEmbeddingModel(String embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  public String getEmbeddingTextVersion() {
    return embeddingTextVersion;
  }

  public void setEmbeddingTextVersion(String embeddingTextVersion) {
    this.embeddingTextVersion = embeddingTextVersion;
  }

  public float[] getEmbedding() {
    return embedding;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }
}
