package me.nghlong3004.iom.api.common;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Centralized access to externalized bot reply messages. Resolves message keys from {@code
 * messages.properties} via Spring's {@link MessageSource}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
@RequiredArgsConstructor
public class BotMessages {

  private static final Locale DEFAULT_LOCALE = Locale.of("vi", "VN");

  private final MessageSource messageSource;

  public String get(String key, Object... args) {
    return messageSource.getMessage(key, args, DEFAULT_LOCALE);
  }

  public String fallbackMessage() {
    return get("bot.fallback.message");
  }

  public String startMessage() {
    return get("bot.start.message");
  }

  public String helpMessage() {
    return get("bot.help.message");
  }

  public String unknownCommandMessage() {
    return get("bot.unknown-command.message");
  }

  public String transactionRecorded(String typeLabel, String formattedAmount, String note) {
    if (note == null || note.isBlank()) {
      return get("bot.transaction.recorded.no-note", typeLabel, formattedAmount);
    }
    return get("bot.transaction.recorded", typeLabel, formattedAmount, note);
  }

  public String typeLabel(boolean isIncome) {
    return isIncome ? get("bot.transaction.type.income") : get("bot.transaction.type.expense");
  }

  public String summaryEmpty(String label) {
    return get("bot.summary.empty", label);
  }

  public String summaryLine(String expense, String income, String currencyName) {
    return get("bot.summary.line", expense, income, currencyName);
  }

  public String summaryExpenseLine(String expense, String currencyName) {
    return get("bot.summary.expense-line", expense, currencyName);
  }

  public String summaryIncomeLine(String income, String currencyName) {
    return get("bot.summary.income-line", income, currencyName);
  }

  public String summaryClarification(String clarificationMessage) {
    return get("bot.summary.clarification", clarificationMessage);
  }

  public String summaryTotal(int count) {
    return get("bot.summary.total", count);
  }

  public String todayLabel() {
    return get("bot.summary.label.today");
  }

  public String monthLabel(int month, int year) {
    return get("bot.summary.label.month", month, year);
  }

  public String detailHeader(String label) {
    return get("bot.detail.header", label);
  }

  public String detailLine(int index, String emoji, String note, String typeLabel, String formattedAmount) {
    return get("bot.detail.line", index, emoji, note, typeLabel, formattedAmount);
  }

  public String detailEmpty(String label) {
    return get("bot.detail.empty", label);
  }

  public String compactSeparator() {
    return get("bot.compact.separator");
  }
}
