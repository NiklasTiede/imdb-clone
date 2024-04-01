package com.thecodinglab.imdbclone.enums;

/** Sets of MovieGenreEnums are persisted as bit values */
public enum MovieGenreEnum {
  HORROR(1 << 1),
  MYSTERY(1 << 2),
  THRILLER(1 << 3),
  CRIME(1 << 4),
  WESTERN(1 << 5),
  WAR(1 << 6),
  ACTION(1 << 7),
  ADVENTURE(1 << 8),
  FAMILY(1 << 9),
  COMEDY(1 << 10),
  ANIMATION(1 << 11),
  FANTASY(1 << 12),
  SCI_FI(1 << 13),
  DRAMA(1 << 14),
  ROMANCE(1 << 15),
  SPORT(1 << 16),
  HISTORY(1 << 17),
  BIOGRAPHY(1 << 18),
  MUSIC(1 << 19),
  MUSICAL(1 << 20),
  DOCUMENTARY(1 << 21),
  NEWS(1 << 22),
  ADULT(1 << 23),
  REALITY_TV(1 << 24),
  TALK_SHOW(1 << 25),
  GAME_SHOW(1 << 26),
  FILM_NOIR(1 << 27),
  SHORT(1 << 28);

  private final int id;

  MovieGenreEnum(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
