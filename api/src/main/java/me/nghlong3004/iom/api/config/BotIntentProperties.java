package me.nghlong3004.iom.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keyword configuration for deterministic bot intents.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ConfigurationProperties(prefix = "iom.bot.intents")
public record BotIntentProperties(Summary summary) {

  public record Summary(
      List<String> actionKeywords,
      List<String> todayKeywords,
      List<String> yesterdayKeywords,
      List<String> dayBeforeKeywords,
      List<String> thisWeekKeywords,
      List<String> monthKeywords,
      List<String> expenseKeywords,
      List<String> incomeKeywords,
      List<String> detailKeywords) {}
}
