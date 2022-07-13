package com.example.demo.enums;

public enum MovieTypeEnum {
  SHORT(1, "Short"),
  MOVIE(2, "Movie"),
  VIDEO(3, "Video"),
  TV_MOVIE(4, "TV Movie"),
  TV_EPISODE(5, "TV Episode"),
  TV_MINI_SERIES(6, "TV Mini Series"),
  TV_SPECIAL(7, "TV Movie"),
  TV_SERIES(8, "TV Series"),
  TV_SHORT(9, "TV Short"),
  TV_PILOT(10, "TV Pilot"),
  VIDEO_GAME(11, "Video Game");

  private int id;
  private String name;

  MovieTypeEnum(int i, String movie) {}

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
