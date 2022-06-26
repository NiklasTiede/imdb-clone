package com.example.demo.entity;

import java.util.List;
import javax.persistence.*;

@Entity
public class Movie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String title;
  private String year;

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

  public Movie(String title, String year) {
    this.title = title;
    this.year = year;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  @Override
  public String toString() {
    return "Movie{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", year='"
        + year
        + '\''
        + ", userratings="
        + ratings
        + '}';
  }

  // should I update like this or differently?
  public void update(String title, String year) {
    if (title != null) {
      this.title = title;
    }
    if (year != null) {
      this.year = year;
      //            this.updatedAt = new DateTime();
    }
  }

  public void update(String title) {
    if (title != null) {
      this.title = title;
    }
  }
}
