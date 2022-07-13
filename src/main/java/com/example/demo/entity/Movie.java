package com.example.demo.entity;

import com.example.demo.enums.attributeconverter.MovieGenreConverterImpl;
import com.example.demo.enums.MovieGenreEnum;
import com.example.demo.enums.MovieTypeEnum;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "MOVIES")
public class Movie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;


  // measure length of strings in notebook over dataset, to adjust varchar length
  private String title;
  private String year;

  private String primaryTitle;
  private String originalTitle;
  private Date startYear;

  private String tconst;

  // check if date is converted accurately
  private Date mofidiedAt;
  private Date createdAt;

  // TODO: test and check if my customer AttributeConverter works! check if the Convert annotation is really needed, or if it is doen automatically
  // advantage: this conversion also works in JPQL statements (handwritten queries in repository)
  @Convert(converter = MovieGenreConverterImpl.class)
  private MovieGenreEnum movieGenreEnum;

  // check if numbers are mapped to enum
  // maybe enums implement somethign identifiable/enumwithId with makes the conversion magic! xxxEnum implements Identifiable<Integer>
  private MovieTypeEnum movieTypeEnum;

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
