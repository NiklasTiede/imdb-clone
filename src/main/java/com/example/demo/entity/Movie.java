package com.example.demo.entity;

import com.example.demo.enums.MovieTypeEnum;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "MOVIE")
public class Movie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  //  private String title;
  //  private String year;

  private String primaryTitle;
  private String originalTitle;
  private Integer startYear;
  private Date mofidiedAt;
  private Date createdAt;
  private Integer movieGenreEnum; // convert multiple values into bit values!
  private MovieTypeEnum movieTypeEnum;
  private Float imdbRating;
  private Integer imdbRatingsCount;

  @OneToMany(
      cascade = CascadeType.ALL,
      mappedBy = "movie",
      fetch = FetchType.EAGER) // otherwise I cannot search for movies with LIKE
  private List<Rating> ratings;

  //    @JsonManagedReference
  public List<Rating> getRatings() {
    return ratings;
  }

  public void setRatings(List<Rating> ratings) {
    this.ratings = ratings;
  }

  public Movie() {}

  public int getId() {
    return id;
  }

  public void setId(int id) {
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

  public Date getMofidiedAt() {
    return mofidiedAt;
  }

  public void setMofidiedAt(Date mofidiedAt) {
    this.mofidiedAt = mofidiedAt;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Integer getMovieGenreEnum() {
    return movieGenreEnum;
  }

  public void setMovieGenreEnum(Integer movieGenreEnum) {
    this.movieGenreEnum = movieGenreEnum;
  }

  public MovieTypeEnum getMovieTypeEnum() {
    return movieTypeEnum;
  }

  public void setMovieTypeEnum(MovieTypeEnum movieTypeEnum) {
    this.movieTypeEnum = movieTypeEnum;
  }

  public Float getImdbRating() {
    return imdbRating;
  }

  public void setImdbRating(Float imdbRating) {
    this.imdbRating = imdbRating;
  }

  public Integer getImdbRatingsCount() {
    return imdbRatingsCount;
  }

  public void setImdbRatingsCount(Integer imdbRatingsCount) {
    this.imdbRatingsCount = imdbRatingsCount;
  }

  //  public Movie(String title, String year) {
  //    this.title = title;
  //    this.year = year;
  //  }
  //
  //  public int getId() {
  //    return id;
  //  }
  //
  //  public void setId(int id) {
  //    this.id = id;
  //  }
  //
  //  public String getTitle() {
  //    return title;
  //  }
  //
  //  public void setTitle(String title) {
  //    this.title = title;
  //  }
  //
  //  public String getYear() {
  //    return year;
  //  }
  //
  //  public void setYear(String year) {
  //    this.year = year;
  //  }
  //
  //  @Override
  //  public String toString() {
  //    return "Movie{"
  //        + "id="
  //        + id
  //        + ", title='"
  //        + title
  //        + '\''
  //        + ", year='"
  //        + year
  //        + '\''
  //        + ", userratings="
  //        + ratings
  //        + '}';
  //  }
  //
  //  // should I update like this or differently?
  //  public void update(String title, String year) {
  //    if (title != null) {
  //      this.title = title;
  //    }
  //    if (year != null) {
  //      this.year = year;
  //      //            this.updatedAt = new DateTime();
  //    }
  //  }
  //
  //  public void update(String title) {
  //    if (title != null) {
  //      this.title = title;
  //    }
  //  }
}
