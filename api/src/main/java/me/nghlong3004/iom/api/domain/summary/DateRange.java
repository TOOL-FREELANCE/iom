package me.nghlong3004.iom.api.domain.summary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Objects;

/**
 * Immutable value object representing a date range with a human-readable label. Provides factory
 * methods for common relative date expressions.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public record DateRange(Instant from, Instant to, String label) {

  public DateRange {
    Objects.requireNonNull(from, "from is required");
    Objects.requireNonNull(to, "to is required");
    if (!from.isBefore(to)) {
      throw new IllegalArgumentException("from must be before to");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("label is required");
    }
  }

  public static DateRange today(ZoneId zone) {
    var today = LocalDate.now(zone);
    return ofDay(today, "Hôm nay", zone);
  }

  public static DateRange yesterday(ZoneId zone) {
    var yesterday = LocalDate.now(zone).minusDays(1);
    return ofDay(yesterday, "Hôm qua", zone);
  }

  public static DateRange daysAgo(int n, String label, ZoneId zone) {
    var day = LocalDate.now(zone).minusDays(n);
    return ofDay(day, label, zone);
  }

  public static DateRange thisWeek(ZoneId zone) {
    var today = LocalDate.now(zone);
    var monday = today.with(ChronoField.DAY_OF_WEEK, 1);
    var from = monday.atStartOfDay(zone).toInstant();
    var to = today.plusDays(1).atStartOfDay(zone).toInstant();
    return new DateRange(from, to, "Tuần này");
  }

  public static DateRange thisMonth(ZoneId zone) {
    var currentMonth = YearMonth.now(zone);
    var from = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
    var to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
    return new DateRange(from, to, "Tháng " + currentMonth.getMonthValue() + "/" + currentMonth.getYear());
  }

  public static DateRange month(int monthValue, int year, ZoneId zone) {
    var ym = YearMonth.of(year, monthValue);
    var from = ym.atDay(1).atStartOfDay(zone).toInstant();
    var to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
    return new DateRange(from, to, "Tháng " + monthValue + "/" + year);
  }

  public static DateRange custom(Instant from, Instant to, String label) {
    return new DateRange(from, to, label);
  }

  private static DateRange ofDay(LocalDate day, String label, ZoneId zone) {
    var from = day.atStartOfDay(zone).toInstant();
    var to = day.plusDays(1).atStartOfDay(zone).toInstant();
    return new DateRange(from, to, label);
  }
}
