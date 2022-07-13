
import pandas as pd
import numpy as np
from utilities import MovieGenreEnum, read_enum

df = pd.read_csv("imdb-dataset/movies.tsv", sep='\t')


counter = 0

for field in df["genres"]:
    if type(field) == str:
        counter += 1
        enums = field.split(",")
        enum_lst = []
        for enum in enums:
            enum = enum.upper()
            real_enum = read_enum(enum)
            enum_lst.append(real_enum)
        # convert enum list into bitvalue and store it
        print(enum_lst)
        bit_value = MovieGenreEnum.enumsToBitValue(enum_lst)
        print(bit_value)
        if counter == 20:
            break


def convert_str_to_bitvalue(field_value):
    if type(field) == str:
        enums = field.split(",")
        enum_lst = []
        for enum in enums:
            enum = enum.upper()
            real_enum = read_enum(enum)
            enum_lst.append(real_enum)
        return MovieGenreEnum.enumsToBitValue(enum_lst)


