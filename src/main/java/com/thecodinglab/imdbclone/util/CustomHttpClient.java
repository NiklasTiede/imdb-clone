package com.thecodinglab.imdbclone.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpMethod;

public class CustomHttpClient {

  static ObjectMapper objectMapper = new ObjectMapper();

  public static <T> T makeRequest(HttpMethod requestType) {

    switch (requestType) {
      case GET:
        return null;
      case POST:
        return null;
      case PUT:
        return null;
      case DELETE:
        return null;
      default:
        return null;
    }
  }
  // GET request, method overloading for multiple path vars / request porams ?
  public static <T> T makeRequest(
      HttpMethod httpMethod,
      String baseUrl,
      String uri,
      Object requestObject,
      Class<T> responseObjectType) {
    String requestBody = null;
    try {
      requestBody = objectMapper.writeValueAsString(requestObject);
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .method(httpMethod.toString(), HttpRequest.BodyPublishers.ofString(requestBody))
              .uri(URI.create(baseUrl + uri))
              .header("Content-Type", "application/json")
              .timeout(Duration.of(60, ChronoUnit.SECONDS))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper.readValue(response.body(), responseObjectType);
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
