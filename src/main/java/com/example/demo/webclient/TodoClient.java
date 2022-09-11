package com.example.demo.webclient;

import com.example.demo.elasticsearch.Todo;
import com.example.demo.util.CustomHttpClient;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class TodoClient {

  // later injected from .properties files (if service has INT/PROD env)
  String baseUrl = "https://jsonplaceholder.typicode.com";

  public Todo getFirstEndpoint(Todo todo) {
    return CustomHttpClient.makeRequest(
        HttpMethod.POST, baseUrl, "https://jsonplaceholder.typicode.com/posts", todo, Todo.class);
  }

  // more endpoints

  // how to add api keys?

}
