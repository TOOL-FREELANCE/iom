package me.nghlong3004.iom.api.domain.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FinanceQuery Unit Tests")
class FinanceQueryTest {

  @Test
  @DisplayName("View should default null flowFilter to ALL")
  void view_NullFlowFilter_DefaultsToAll() {
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-05-02T00:00:00Z");
    var range = DateRange.custom(from, to, "Test");

    var view = new FinanceQuery.View(range, null, null);

    assertThat(view.flowFilter()).isEqualTo(FlowFilter.ALL);
    assertThat(view.viewMode()).isEqualTo(ViewMode.SUMMARY);
  }

  @Test
  @DisplayName("View should reject null dateRange")
  void view_NullDateRange_ThrowsNpe() {
    assertThatThrownBy(() -> new FinanceQuery.View(null, FlowFilter.ALL, ViewMode.SUMMARY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("dateRange is required");
  }

  @Test
  @DisplayName("Clarification should reject blank message")
  void clarification_BlankMessage_ThrowsException() {
    assertThatThrownBy(() -> new FinanceQuery.Clarification(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clarification message is required");
  }

  @Test
  @DisplayName("Clarification should store message")
  void clarification_ValidMessage_StoresMessage() {
    var clarification = new FinanceQuery.Clarification("Bạn muốn xem ngày nào?");
    assertThat(clarification.message()).isEqualTo("Bạn muốn xem ngày nào?");
  }

  @Test
  @DisplayName("View should preserve explicit values")
  void view_ExplicitValues_Preserved() {
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-05-02T00:00:00Z");
    var range = DateRange.custom(from, to, "Test");

    var view = new FinanceQuery.View(range, FlowFilter.EXPENSE, ViewMode.DETAIL);

    assertThat(view.flowFilter()).isEqualTo(FlowFilter.EXPENSE);
    assertThat(view.viewMode()).isEqualTo(ViewMode.DETAIL);
  }
}
