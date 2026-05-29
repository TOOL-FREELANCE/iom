package me.nghlong3004.iom.api.application.handler.command;

import java.time.ZoneId;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.summary.DateRange;

/**
 * Enumerates recognized bot slash commands. Commands with a non-null {@link #dateRangeFactory} are
 * summary commands handled by {@link SummaryCommandHandler}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Getter
@RequiredArgsConstructor
public enum BotCommand {
  START("/start", null),
  HELP("/help", null),
  TODAY("/today", DateRange::today),
  MONTH("/month", DateRange::thisMonth),
  ;

  private final String command;
  private final Function<ZoneId, DateRange> dateRangeFactory;

  /**
   * Returns {@code true} if this command produces a financial summary view.
   */
  public boolean hasSummary() {
    return dateRangeFactory != null;
  }

  /**
   * Creates the date range for this summary command.
   *
   * @throws IllegalStateException if this command has no summary factory
   */
  public DateRange createDateRange(ZoneId zone) {
    if (dateRangeFactory == null) {
      throw new IllegalStateException("Command " + name() + " has no summary factory");
    }
    return dateRangeFactory.apply(zone);
  }
}
