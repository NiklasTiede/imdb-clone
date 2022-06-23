package com.example.demo.controller;

import com.example.demo.AbstractTest;
import com.example.demo.entity.Movie;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class MovieControllerTest extends AbstractTest {

    // when wanna print stuff: this lib is useful!
    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void getMovieList() throws Exception {
        String uri = "/movies";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                                                                .accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        String content = mvcResult.getResponse().getContentAsString();
        Movie[] movieList = super.mapFromJson(content, Movie[].class);

        Assertions.assertTrue(movieList.length > 0);
        Assertions.assertTrue(movieList.length > 900);

        System.out.println(movieList.length);
        systemOutRule.getLog();
    }

}