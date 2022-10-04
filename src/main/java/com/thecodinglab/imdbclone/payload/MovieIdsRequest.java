package com.thecodinglab.imdbclone.payload;

import java.util.List;

public record MovieIdsRequest(

        List<Long> movieIds
) {}
