package me.nghlong3004.iom.api.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.DateRangeResolver;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import org.springframework.stereotype.Component;

/**
 * Orchestrates a chain of {@link DateRangeResolver} implementations. Tries each resolver in
 * {@link org.springframework.core.annotation.Order} sequence and returns the first successful
 * result.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Component
@RequiredArgsConstructor
public class DateRangeResolverChain {

  private final List<DateRangeResolver> resolvers;

  /**
   * Resolves a date range by trying each resolver in order.
   *
   * @param normalizedText the user's message text
   * @return the first successfully resolved date range, or empty if no resolver matched
   */
  public Optional<DateRange> resolve(String normalizedText) {
    return resolvers.stream()
        .map(resolver -> resolver.resolve(normalizedText))
        .flatMap(Optional::stream)
        .findFirst();
  }
}
