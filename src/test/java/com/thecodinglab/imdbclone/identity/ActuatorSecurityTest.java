package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.support.BaseContainers;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "management.server.port=0",
      "management.metrics.distribution.percentiles-histogram[http.server.requests]=true"
    })
class ActuatorSecurityTest extends BaseContainers {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort private int serverPort;
  @LocalManagementPort private int managementPort;

  @Test
  void prometheusEndpoint_allowsAnonymousScrapes() throws IOException, InterruptedException {
    get(serverPort, "/api/movie/1");

    HttpResponse<String> response = get(managementPort, "/actuator/prometheus");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("# HELP");
    assertThat(response.body()).contains("http_server_requests_seconds_bucket");
  }

  @Test
  void nonPublicActuatorEndpoint_requiresAuthentication() throws IOException, InterruptedException {
    HttpResponse<String> response = get(managementPort, "/actuator/env");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
            .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
