package app.popcornsociety.assistant.internal.mcp;

import app.popcornsociety.engagement.api.AssistantActionReceipt;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalActionResult(
    String contractVersion,
    String operationId,
    Long movieId,
    String kind,
    boolean changed,
    @Nullable BigDecimal score,
    @Nullable BigDecimal previousScore) {
  static PersonalActionResult from(AssistantActionReceipt receipt) {
    return new PersonalActionResult(
        "1.0",
        receipt.operationId().toString(),
        receipt.movieId(),
        receipt.kind(),
        receipt.changed(),
        receipt.score(),
        receipt.previousScore());
  }
}
