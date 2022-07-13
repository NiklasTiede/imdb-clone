package com.example.demo.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MovieGenreEnum {
  HORROR(1 << 1, "Horror"),
  MYSTERY(1 << 2, "Mystery"),
  THRILLER(1 << 3, "Thriller"),
  CRIME(1 << 4, "Crime"),
  WESTERN(1 << 5, "Western"),
  WAR(1 << 6, "War"),
  ACTION(1 << 7, "Action"),
  ADVENTURE(1 << 8, "Adventure"),
  FAMILY(1 << 9, "Family"),
  COMEDY(1 << 10, "Comedy"),
  ANIMATION(1 << 11, "Animation"),
  FANTASY(1 << 12, "Fantasy"),
  SCI_FI(1 << 13, "Sci-Fi"),
  DRAMA(1 << 14, "Drama"),
  ROMANCE(1 << 15, "Romance"),
  SPORT(1 << 16, "Sport"),
  HISTORY(1 << 17, "History"),
  BIOGRAPHY(1 << 18, "Biography"),
  MUSIC(1 << 19, "Music"),
  MUSICAL(1 << 20, "Adult"),
  DOCUMENTARY(1 << 21, "Documentary"),
  NEWS(1 << 22, "News"),
  ADULT(1 << 23, "Adult"),
  REALITY_TV(1 << 24, "Reality TV"),
  TALK_SHOW(1 << 25, "Talk Show"),
  GAME_SHOW(1 << 26, "Game Show"),
  FILM_NOIR(1 << 27, "Film Noir"),
  SHORT(1 << 28, "Short");

  private int id;
  private String name;

  MovieGenreEnum(int id, String name) {
    this.id = id;
    this.name = name;
  }

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

  public static Long enumToBitValue(Set<MovieGenreEnum> movieGenreEnumSet) {
    long bitValue = 1L;
    for (MovieGenreEnum e : movieGenreEnumSet) {
      bitValue |= e.getId();
    }
    return bitValue;
  }

  public static Set<MovieGenreEnum> bitValueToEnum(Long bitValue) {
    return Arrays.stream(MovieGenreEnum.values())
        .filter(singleEnum -> (bitValue & singleEnum.getId()) != 0)
        .collect(Collectors.toSet());
  }
}
