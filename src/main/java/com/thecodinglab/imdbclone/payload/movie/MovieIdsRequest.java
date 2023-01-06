package com.thecodinglab.imdbclone.payload.movie;

import java.util.List;

public record MovieIdsRequest(
        List<Long> movieIds
) {}
