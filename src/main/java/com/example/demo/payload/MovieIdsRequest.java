package com.example.demo.payload;

import java.util.List;

public record MovieIdsRequest(

        List<Long> movieIds
) {}
