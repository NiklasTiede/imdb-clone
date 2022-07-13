from enum import Enum
import numpy as np

class MovieGenreEnum(Enum):
    HORROR = 1 << 1
    MYSTERY = 1 << 2
    THRILLER = 1 << 3
    CRIME = 1 << 4
    WESTERN = 1 << 5
    WAR = 1 << 6
    ACTION = 1 << 7
    ADVENTURE = 1 << 8
    FAMILY = 1 << 9
    COMEDY = 1 << 10
    ANIMATION = 1 << 11
    FANTASY = 1 << 12
    SCI_FI = 1 << 13
    DRAMA = 1 << 14
    ROMANCE = 1 << 15
    SPORT = 1 << 16
    HISTORY = 1 << 17
    BIOGRAPHY = 1 << 18
    MUSIC = 1 << 19
    MUSICAL = 1 << 20
    DOCUMENTARY = 1 << 21
    NEWS = 1 << 22
    ADULT = 1 << 23
    REALITY_TV = 1 << 24
    TALK_SHOW = 1 << 25
    GAME_SHOW = 1 << 26
    FILM_NOIR = 1 << 27
    SHORT = 1 << 28
        
    def enumsToBitValue(movieenums):
        bit_value = 1
        for enum2 in movieenums:
            if enum2 in MovieGenreEnum:
                bit_value |= enum2.value
        return bit_value
    


class MovieTypeEnum(Enum):
    SHORT = 1
    MOVIE = 2
    VIDEO = 3
    TV_MOVIE = 4
    TV_EPISODE = 5
    TV_MINI_SERIES = 6
    TV_SPECIAL = 7
    TV_SERIES = 8
    TV_SHORT = 9
    TV_PILOT = 10
    VIDEO_GAME = 11


movietype_map = {
    "movie": MovieTypeEnum.MOVIE.value,
    "tvEpisode": MovieTypeEnum.TV_EPISODE.value,
    "tvSeries": MovieTypeEnum.TV_SERIES.value,
    "tvShort": MovieTypeEnum.TV_SHORT.value,
    "tvMovie": MovieTypeEnum.TV_MOVIE.value,
    "tvMiniSeries": MovieTypeEnum.TV_MINI_SERIES.value,
    "tvSpecial": MovieTypeEnum.TV_SPECIAL.value,
    "video": MovieTypeEnum.VIDEO.value,
    "videoGame": MovieTypeEnum.VIDEO_GAME.value,
    "tvPilot": MovieTypeEnum.TV_PILOT.value,
    "short": MovieTypeEnum.SHORT.value,
}

def convert_enum_to_number(field):
    if type(field) == str:
        return movietype_map[field]
            


def read_enum(enum_string):
    enum_existent = False
    for enum in MovieGenreEnum:
        if enum.name == enum_string:
            enum_existent = True
            return enum
    if not enum_existent:
        print("enum-string could not be converted to an Enum!")
        print(enum_string)



def convert_str_to_bitvalue(field_value):
    if type(field_value) == str:
        enums = field_value.split(",")
        enum_lst = []
        for enum in enums:
            enum = enum.upper().replace('-', '_')
            real_enum = read_enum(enum)
            enum_lst.append(real_enum)
        return MovieGenreEnum.enumsToBitValue(enum_lst)
    if type(field_value) == int or type(field_value) == float:
        return np.NaN


def convert_tconst_to_indexnum(field_value):
    field_value = field_value.replace('tt', '')
    return int(field_value)


