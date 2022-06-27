package com.example.demo.enums;

public enum MovieGenreEnum {
  HORROR(1, "Horror"),
  MYSTERY(2, "Mystery"),
  THRILLER(3, "Thriller"),
  CRIME(4, "Crime"),
  WESTERN(5, "Western"),
  WAR(6, "War"),
  ACTION(7, "Action"),
  ADVENTURE(8, "Adventure"),
  FAMILY(9, "Family"),
  COMEDY(10, "Comedy"),
  ANIMATION(11, "Animation"),
  FANTASY(12, "Fantasy"),
  SCI_FI(13, "Sci-Fi"),
  DRAMA(14, "Drama"),
  ROMANCE(15, "Romance"),
  SPORT(16, "Sport"),
  HISTORY(17, "History"),
  BIOGRAPHY(18, "Biography"),
  MUSIC(19, "Music"),
  DOCUMENTARY(20, "Documentary"),
  SHORT(21, "Short"),
  NEWS(22, "News");

  MovieGenreEnum(int id, String name) {
  }

  MovieGenreEnum() {
  }

}
