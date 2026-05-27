package me.nghlong3004.iom.api.domain.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DateRange Unit Tests")
class DateRangeTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Test
  @DisplayName("Should reject null from")
  void constructor_NullFrom_ThrowsNpe() {
    assertThatThrownBy(() -> new DateRange(null, Instant.now(), "label"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("from is required");
  }

  @Test
  @DisplayName("Should reject null to")
  void constructor_NullTo_ThrowsNpe() {
    assertThatThrownBy(() -> new DateRange(Instant.now(), null, "label"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("to is required");
  }

  @Test
  @DisplayName("Should reject from >= to")
  void constructor_FromNotBeforeTo_ThrowsException() {
    var now = Instant.now();
    assertThatThrownBy(() -> new DateRange(now, now, "label"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("from must be before to");
  }

  @Test
  @DisplayName("Should reject blank label")
  void constructor_BlankLabel_ThrowsException() {
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-05-02T00:00:00Z");
    assertThatThrownBy(() -> new DateRange(from, to, "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("label is required");
  }

  @Test
  @DisplayName("today() should return a single-day range for today")
  void today_ReturnsCurrentDayRange() {
    var range = DateRange.today(ZONE);
    var today = LocalDate.now(ZONE);
    var expectedFrom = today.atStartOfDay(ZONE).toInstant();
    var expectedTo = today.plusDays(1).atStartOfDay(ZONE).toInstant();

    assertThat(range.from()).isEqualTo(expectedFrom);
    assertThat(range.to()).isEqualTo(expectedTo);
    assertThat(range.label()).isEqualTo("Hôm nay");
  }

  @Test
  @DisplayName("yesterday() should return a single-day range for yesterday")
  void yesterday_ReturnsPreviousDayRange() {
    var range = DateRange.yesterday(ZONE);
    var yesterday = LocalDate.now(ZONE).minusDays(1);
    var expectedFrom = yesterday.atStartOfDay(ZONE).toInstant();

    assertThat(range.from()).isEqualTo(expectedFrom);
    assertThat(range.label()).isEqualTo("Hôm qua");
  }

  @Test
  @DisplayName("daysAgo() should return n days before today")
  void daysAgo_ReturnsDaysAgoRange() {
    var range = DateRange.daysAgo(2, "Hôm kia", ZONE);
    var twoDaysAgo = LocalDate.now(ZONE).minusDays(2);
    var expectedFrom = twoDaysAgo.atStartOfDay(ZONE).toInstant();

    assertThat(range.from()).isEqualTo(expectedFrom);
    assertThat(range.label()).isEqualTo("Hôm kia");
  }

  @Test
  @DisplayName("thisWeek() should start from Monday")
  void thisWeek_StartsFromMonday() {
    var range = DateRange.thisWeek(ZONE);
    assertThat(range.label()).isEqualTo("Tuần này");
    assertThat(range.from()).isBefore(range.to());
  }

  @Test
  @DisplayName("thisMonth() should cover full current month")
  void thisMonth_CoversFullMonth() {
    var range = DateRange.thisMonth(ZONE);
    assertThat(range.label()).startsWith("Tháng ");
    assertThat(range.from()).isBefore(range.to());
  }

  @Test
  @DisplayName("custom() should accept arbitrary from/to/label")
  void custom_ReturnsProvidedValues() {
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-05-21T00:00:00Z");
    var range = DateRange.custom(from, to, "1/5 - 20/5");

    assertThat(range.from()).isEqualTo(from);
    assertThat(range.to()).isEqualTo(to);
    assertThat(range.label()).isEqualTo("1/5 - 20/5");
  }
}
