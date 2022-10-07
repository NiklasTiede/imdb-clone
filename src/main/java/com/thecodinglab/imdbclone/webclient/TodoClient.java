package com.thecodinglab.imdbclone.webclient;

import com.thecodinglab.imdbclone.utility.CustomHttpClient;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class TodoClient {

  // later injected from .properties files (if service has INT/PROD env)
  String baseUrl = "https://jsonplaceholder.typicode.com";

  public Todo getFirstEndpoint(Todo todo) {
    return CustomHttpClient.makeRequest(HttpMethod.POST, baseUrl, "/posts", todo, Todo.class);
  }

  public Todo getSecondEndpoint(Todo todo) {
    return CustomHttpClient.makeRequest(
        HttpMethod.GET, baseUrl, "posts/{postId}", todo, Todo.class);
  }

  // more endpoints

  // how to add api keys?

}
