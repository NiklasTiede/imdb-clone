package com.example.demo.webclient;

import com.example.demo.dto.Todo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface SomeClient {

  ObjectMapper objectMapper = new ObjectMapper();

  static void main(String[] args) throws IOException, InterruptedException {

    String uri = "https://jsonplaceholder.typicode.com/todos/1";

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Todo todo = objectMapper.readValue(response.body(), Todo.class);

    System.out.println(response.body());
    System.out.println(response.statusCode());
    System.out.println(todo);
  }
}
