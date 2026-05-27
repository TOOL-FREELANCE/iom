package me.nghlong3004.iom.api.service;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.DateRangeResolver;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Deterministic {@link DateRangeResolver} that resolves common Vietnamese date expressions using
 * keyword matching from configuration. Runs first in the chain (before LLM fallback).
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class KeywordDateResolver implements DateRangeResolver {

  private final BotIntentProperties botIntentProperties;

  @Override
  public Optional<DateRange> resolve(String normalizedText) {
    if (normalizedText == null || normalizedText.isBlank()) {
      return Optional.empty();
    }

    var text = stripAccents(normalizedText);
    var zone = ZoneId.systemDefault();
    var summary = botIntentProperties.summary();

    if (hasAny(text, summary.todayKeywords())) {
      return Optional.of(DateRange.today(zone));
    }

    if (hasAny(text, summary.yesterdayKeywords())) {
      return Optional.of(DateRange.yesterday(zone));
    }

    if (hasAny(text, summary.dayBeforeKeywords())) {
      return Optional.of(DateRange.daysAgo(2, "Hôm kia", zone));
    }

    if (hasAny(text, summary.thisWeekKeywords())) {
      return Optional.of(DateRange.thisWeek(zone));
    }

    if (hasAny(text, summary.monthKeywords())) {
      return Optional.of(DateRange.thisMonth(zone));
    }

    return Optional.empty();
  }

  private boolean hasAny(String text, Iterable<String> keywords) {
    if (keywords == null) {
      return false;
    }
    for (String keyword : keywords) {
      if (text.contains(stripAccents(keyword))) {
        return true;
      }
    }
    return false;
  }

  private String stripAccents(String text) {
    var decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    return decomposed
        .replaceAll("\\p{M}", "")
        .replace('\u0111', 'd')
        .replace('\u0110', 'D')
        .toLowerCase(Locale.ROOT)
        .trim();
  }
}
