package me.nghlong3004.iom.api.application.port.out;

import java.util.Optional;
import me.nghlong3004.iom.api.domain.summary.DateRange;

/**
 * Resolves natural-language text into a {@link DateRange}. Implementations form a Chain of
 * Responsibility: deterministic keyword matching first, LLM fallback second.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public interface DateRangeResolver {

  /**
   * Attempts to resolve a date range from the given text.
   *
   * @param normalizedText the user's message text (already trimmed and lowered)
   * @return the resolved date range, or empty if this resolver cannot handle the text
   */
  Optional<DateRange> resolve(String normalizedText);
}
