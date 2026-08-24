package com.thecodinglab.imdbclone.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class FrontendTelemetryControllerTest extends BaseControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MeterRegistry meterRegistry;
  @Autowired private PrometheusMeterRegistry prometheusMeterRegistry;

  @Test
  void acceptsAnonymousTelemetryWithoutCsrfAndRecordsOnlyBoundedLabels() throws Exception {
    double before =
        meterRegistry.counter("imdb.frontend.events.accepted", "type", "web_vital").count();

    mockMvc
        .perform(
            post("/api/observability/frontend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "events": [
                        {
                          "type": "WEB_VITAL",
                          "name": "LCP",
                          "value": 1234.5,
                          "rating": "GOOD"
                        },
                        {
                          "type": "WEB_VITAL",
                          "name": "CLS",
                          "value": 0.08,
                          "rating": "GOOD"
                        },
                        {
                          "type": "UI_ACTION",
                          "name": "OPEN_MOVIE",
                          "uiActionOutcome": "EXECUTED"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isNoContent());

    org.assertj.core.api.Assertions.assertThat(
            meterRegistry.counter("imdb.frontend.events.accepted", "type", "web_vital").count())
        .isEqualTo(before + 2.0d);
    org.assertj.core.api.Assertions.assertThat(prometheusMeterRegistry.scrape())
        .contains(
            "imdb_frontend_events_accepted_total",
            "imdb_frontend_ui_actions_total",
            "imdb_frontend_web_vital_cls_score_bucket",
            "imdb_frontend_web_vital_duration_seconds_bucket");
  }

  @Test
  void rejectsMismatchedOrOutOfRangeTelemetry() throws Exception {
    mockMvc
        .perform(
            post("/api/observability/frontend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "events": [
                        {
                          "type": "BROWSER_ERROR",
                          "name": "LCP",
                          "value": 999999,
                          "rating": "POOR"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUiActionsWithoutABoundedOutcome() throws Exception {
    mockMvc
        .perform(
            post("/api/observability/frontend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "events": [
                        {
                          "type": "UI_ACTION",
                          "name": "OPEN_MOVIE"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest());
  }
}
