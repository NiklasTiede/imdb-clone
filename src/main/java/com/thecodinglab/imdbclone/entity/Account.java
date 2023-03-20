package com.thecodinglab.imdbclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.entity.audit.DateAudit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"username"}),
      @UniqueConstraint(columnNames = {"email"})
    })
public class Account extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String username;
  @NotNull private String email;
  @NotNull @JsonIgnore private String password;
  private String firstName;
  private String lastName;
  private String phone;
  private String bio;
  private Date birthday;
  private String imageUrlToken;
  @JsonIgnore private Boolean locked = false;
  @JsonIgnore private Boolean enabled = false;

  @ManyToMany(fetch = FetchType.LAZY)
  private Collection<Role> roles = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<WatchedMovie> watchedMovies;

  @JsonIgnore
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<Comment> comments;

  @JsonIgnore
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<Rating> ratings;

  @JsonIgnore
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<VerificationToken> verificationTokens;

  public Account() {}

  public Account(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday(Date birthday) {
    this.birthday = birthday;
  }

  public String getImageUrlToken() {
    return imageUrlToken;
  }

  public void setImageUrlToken(String imageUrlToken) {
    this.imageUrlToken = imageUrlToken;
  }

  public Boolean getLocked() {
    return locked;
  }

  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Collection<Role> getRoles() {
    return roles;
  }

  public void setRoles(Collection<Role> roles) {
    this.roles = roles;
  }

  public Collection<WatchedMovie> getWatchedMovies() {
    return watchedMovies;
  }

  public void setWatchedMovies(Collection<WatchedMovie> watchedMovies) {
    this.watchedMovies = watchedMovies;
  }

  public Collection<Comment> getComments() {
    return comments;
  }

  public void setComments(Collection<Comment> comments) {
    this.comments = comments;
  }

  public Collection<Rating> getRatings() {
    return ratings;
  }

  public void setRatings(Collection<Rating> ratings) {
    this.ratings = ratings;
  }

  public Collection<VerificationToken> getVerificationTokens() {
    return verificationTokens;
  }

  public void setVerificationTokens(Collection<VerificationToken> verificationTokens) {
    this.verificationTokens = verificationTokens;
  }
}
