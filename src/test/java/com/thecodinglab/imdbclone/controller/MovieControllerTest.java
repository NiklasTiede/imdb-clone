package com.thecodinglab.imdbclone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.MovieIdsRequest;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_getMovieById_returnsOk() throws Exception {

        // arrange
        String uri = "/api/movie/1457767";
        RequestBuilder request = MockMvcRequestBuilders.get(uri);

        // act
        MvcResult result = mockMvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();
        Movie returnedMovie = mapper.readValue(content, Movie.class);

        // assert
        Assertions.assertEquals(status, 200);
        Assertions.assertEquals(returnedMovie.getPrimaryTitle(), "The Conjuring");
        Assertions.assertNotEquals(returnedMovie.getPrimaryTitle(), "NotExistentMovie");
    }

    @Test
    void test_getMoviesByIds_returnsOk() throws Exception {

        // arrange
        String uri = "/api/movie/get-movies";
        List<Long> movieIds = new ArrayList<>();
        movieIds.add(1457767L);
        movieIds.add(1396484L);
        movieIds.add(2872718L);
        MovieIdsRequest getMoviesRequest = new MovieIdsRequest(movieIds);

        // act
        MvcResult result = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(getMoviesRequest)))
                .andReturn();

        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();
        var returnedMoviePages = mapper.readValue(content, PagedResponse.class);

        // assert
        Assertions.assertEquals(status, 200);
        Assertions.assertEquals(returnedMoviePages.getContent().size(), 3);
    }
}