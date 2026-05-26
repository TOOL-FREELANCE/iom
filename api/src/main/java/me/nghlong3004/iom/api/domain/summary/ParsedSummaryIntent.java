package me.nghlong3004.iom.api.domain.summary;

import java.time.Instant;
import java.util.Objects;

/**
 * Parsed natural-language summary request.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
public record ParsedSummaryIntent(
    Instant from,
    Instant to,
    String label,
    FlowFilter flowFilter,
    boolean needsClarification,
    String clarificationMessage) {

  public ParsedSummaryIntent {
    if (needsClarification) {
      if (clarificationMessage == null || clarificationMessage.isBlank()) {
        throw new IllegalArgumentException("clarificationMessage is required");
      }
    } else {
      Objects.requireNonNull(from, "from is required");
      Objects.requireNonNull(to, "to is required");
      if (!from.isBefore(to)) {
        throw new IllegalArgumentException("from must be before to");
      }
      if (label == null || label.isBlank()) {
        throw new IllegalArgumentException("label is required");
      }
      if (flowFilter == null) {
        flowFilter = FlowFilter.ALL;
      }
    }
  }

  public static ParsedSummaryIntent summary(
      Instant from, Instant to, String label, FlowFilter flowFilter) {
    return new ParsedSummaryIntent(from, to, label, flowFilter, false, null);
  }

  public static ParsedSummaryIntent clarification(String message) {
    return new ParsedSummaryIntent(null, null, null, null, true, message);
  }
}
