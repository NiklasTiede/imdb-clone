package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.web.client.RestClient;

/** Fixed-origin, bounded external read. No provider body, credentials or exceptions escape. */
final class TmdbClient {
  private static final int MAX_BYTES = 512 * 1024;
  private final RestClient client;
  private final TmdbProperties properties;
  private final JsonMapper json = JsonMapper.builder().build();
  private final AtomicReference<Instant> blockedUntil = new AtomicReference<>(Instant.MIN);
  private final Semaphore permits = new Semaphore(4);

  record Response(Outcome outcome, Facts facts) {}

  TmdbClient(RestClient.Builder builder, TmdbProperties properties) {
    this.properties = properties;
    builder.baseUrl("https://api.themoviedb.org/3");
    if (properties.enabled()) {
      builder.defaultHeader("Authorization", "Bearer " + properties.readAccessToken());
    }
    client = builder.build();
  }

  boolean enabled() {
    return properties.enabled();
  }

  Response fetch(long tmdbId, String imdbId) {
    var payload = get("/movie/{id}?language=en-US&append_to_response=credits", tmdbId);
    if (payload.outcome() != Outcome.AVAILABLE) return new Response(payload.outcome(), null);
    var data = payload.data();
    if (imdbId != null && !imdbId.isBlank() && !imdbId.equals(data.path("imdb_id").asText()))
      return new Response(Outcome.IDENTITY_MISMATCH, null);
    return new Response(Outcome.AVAILABLE, facts(data));
  }

  record Payload(Outcome outcome, JsonNode data) {}

  Payload watchProviders(long tmdbId) {
    return get("/movie/{id}/watch/providers", tmdbId);
  }

  private Payload get(String path, long tmdbId) {
    if (!enabled()) return new Payload(Outcome.DISABLED, null);
    if (Instant.now().isBefore(blockedUntil.get())) return new Payload(Outcome.RATE_LIMITED, null);
    if (!permits.tryAcquire()) return new Payload(Outcome.UNAVAILABLE, null);
    try {
      return client
          .get()
          .uri(path, tmdbId)
          .exchange(
              (request, response) -> {
                int status = response.getStatusCode().value();
                if (status == 429) {
                  long seconds = 30;
                  try {
                    seconds =
                        Math.clamp(
                            Long.parseLong(response.getHeaders().getFirst("Retry-After")), 1, 300);
                  } catch (NumberFormatException ignored) {
                    // A missing or HTTP-date Retry-After uses the bounded default cooldown.
                  }
                  blockedUntil.set(Instant.now().plusSeconds(seconds));
                  return new Payload(Outcome.RATE_LIMITED, null);
                }
                if (status == 404) return new Payload(Outcome.NOT_FOUND, null);
                if (status != 200) return new Payload(Outcome.UNAVAILABLE, null);
                byte[] body = response.getBody().readNBytes(MAX_BYTES + 1);
                if (body.length > MAX_BYTES) return new Payload(Outcome.UNAVAILABLE, null);
                JsonNode data = json.readTree(body);
                if (data == null || !data.isObject()) return new Payload(Outcome.UNAVAILABLE, null);
                if (!data.path("id").isIntegralNumber()
                    || !data.path("id").canConvertToLong()
                    || data.path("id").asLong() != tmdbId) {
                  return new Payload(Outcome.IDENTITY_MISMATCH, null);
                }
                return new Payload(Outcome.AVAILABLE, data);
              });
    } catch (RuntimeException ignored) {
      return new Payload(Outcome.UNAVAILABLE, null);
    } finally {
      permits.release();
    }
  }

  private static Facts facts(JsonNode data) {
    var cast = new ArrayList<CastMember>();
    for (JsonNode member : data.path("credits").path("cast")) {
      String name = text(member, "name");
      if (name != null) cast.add(new CastMember(name, text(member, "character")));
      if (cast.size() == 8) break;
    }
    return new Facts(
        text(data, "tagline"),
        text(data, "release_date"),
        List.copyOf(cast),
        crew(data, Set.of("Director"), 4),
        crew(data, Set.of("Writer", "Screenplay", "Story"), 6),
        names(data.path("production_countries"), "name"),
        names(data.path("production_companies"), "name"),
        names(data.path("spoken_languages"), "english_name"),
        money(data, "budget"),
        money(data, "revenue"));
  }

  private static List<String> crew(JsonNode data, Set<String> jobs, int limit) {
    var names = new ArrayList<String>();
    for (JsonNode member : data.path("credits").path("crew")) {
      String name = text(member, "name");
      if (name != null && jobs.contains(member.path("job").asText()) && !names.contains(name))
        names.add(name);
      if (names.size() == limit) break;
    }
    return List.copyOf(names);
  }

  private static List<String> names(JsonNode data, String field) {
    var names = new ArrayList<String>();
    for (JsonNode item : data) {
      String name = text(item, field);
      if (name != null && !names.contains(name)) names.add(name);
      if (names.size() == 6) break;
    }
    return List.copyOf(names);
  }

  private static String text(JsonNode data, String field) {
    JsonNode value = data.path(field);
    if (!value.isTextual() || value.asText().isBlank()) return null;
    String text = value.asText().strip();
    return text.substring(0, Math.min(text.length(), 200));
  }

  private static Long money(JsonNode data, String field) {
    JsonNode value = data.path(field);
    return value.isIntegralNumber() && value.canConvertToLong() && value.asLong() > 0
        ? value.asLong()
        : null;
  }
}
