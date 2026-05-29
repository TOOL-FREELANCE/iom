package me.nghlong3004.iom.api.domain.summary;

import java.util.Objects;

/**
 * Sealed hierarchy representing a fully-parsed finance query from user input.
 *
 * <p>Use exhaustive {@code switch} expressions for compiler-verified handling:
 *
 * <pre>{@code
 * switch (query) {
 *     case FinanceQuery.View v -> handleView(v);
 *     case FinanceQuery.Clarification c -> handleClarification(c);
 * }
 * }</pre>
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public sealed interface FinanceQuery {

  /** A fully-resolved finance view request. */
  record View(DateRange dateRange, FlowFilter flowFilter, ViewMode viewMode)
      implements FinanceQuery {

    public View {
      Objects.requireNonNull(dateRange, "dateRange is required");
      if (flowFilter == null) {
        flowFilter = FlowFilter.ALL;
      }
      if (viewMode == null) {
        viewMode = ViewMode.SUMMARY;
      }
    }
  }

  /** An ambiguous request that requires user clarification. */
  record Clarification(String message) implements FinanceQuery {

    public Clarification {
      if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("clarification message is required");
      }
    }
  }
}
