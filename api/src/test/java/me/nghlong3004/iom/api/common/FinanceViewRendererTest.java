package me.nghlong3004.iom.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("FinanceViewRenderer Unit Tests")
@ExtendWith(MockitoExtension.class)
class FinanceViewRendererTest {

  @Mock private BotMessages botMessages;
  @InjectMocks private FinanceViewRenderer renderer;

  private static final DateRange RANGE =
      DateRange.custom(
          Instant.parse("2026-05-26T00:00:00Z"),
          Instant.parse("2026-05-27T00:00:00Z"),
          "Hôm qua");

  @Test
  @DisplayName("SUMMARY mode should return summary totals")
  void render_SummaryMode_ReturnsTotals() {
    var summary =
        new TransactionSummary(
            Map.of(Currency.VND, new TransactionSummary.CurrencyTotal(0L, 30000L)), 1);
    given(botMessages.summaryLine(anyString(), anyString(), eq("VND")))
        .willReturn("  Chi 30.000đ | Thu 0đ (VND)");
    given(botMessages.summaryTotal(1)).willReturn("Tổng: 1 giao dịch.");

    var result = renderer.render(RANGE, ViewMode.SUMMARY, List.of(), summary, FlowFilter.ALL);

    assertThat(result).contains("Hôm qua:").contains("Chi 30.000đ").contains("Tổng: 1 giao dịch.");
  }

  @Test
  @DisplayName("SUMMARY mode with zero transactions should return empty")
  void render_SummaryEmpty_ReturnsEmptyMessage() {
    var summary = new TransactionSummary(Map.of(), 0);
    given(botMessages.summaryEmpty("Hôm qua")).willReturn("Hôm qua: Chưa có giao dịch nào.");

    var result = renderer.render(RANGE, ViewMode.SUMMARY, List.of(), summary, FlowFilter.ALL);

    assertThat(result).isEqualTo("Hôm qua: Chưa có giao dịch nào.");
  }

  @Test
  @DisplayName("DETAIL mode should list individual transactions")
  void render_DetailMode_ListsTransactions() {
    var tx = buildTransaction(TransactionType.EXPENSE, 30000L, "ăn sáng", Category.FOOD);
    given(botMessages.detailHeader("Hôm qua")).willReturn("📋 Hôm qua:");
    given(botMessages.detailLine(eq(1), eq("🍜"), eq("ăn sáng"), anyString(), anyString()))
        .willReturn("  1. 🍜 ăn sáng — Chi 30.000đ");
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.summaryTotal(1)).willReturn("Tổng: 1 giao dịch.");

    var result = renderer.render(RANGE, ViewMode.DETAIL, List.of(tx), null, FlowFilter.ALL);

    assertThat(result).contains("📋 Hôm qua:").contains("🍜 ăn sáng").contains("Tổng: 1 giao dịch.");
  }

  @Test
  @DisplayName("DETAIL mode with empty transactions should return empty")
  void render_DetailEmpty_ReturnsEmptyMessage() {
    given(botMessages.detailEmpty("Hôm qua")).willReturn("Hôm qua: Chưa có giao dịch nào.");

    var result = renderer.render(RANGE, ViewMode.DETAIL, List.of(), null, FlowFilter.ALL);

    assertThat(result).isEqualTo("Hôm qua: Chưa có giao dịch nào.");
  }

  @Test
  @DisplayName("COMPACT mode should show list + separator + totals")
  void render_CompactMode_ShowsListAndTotals() {
    var tx = buildTransaction(TransactionType.EXPENSE, 30000L, "ăn sáng", Category.FOOD);
    var summary =
        new TransactionSummary(
            Map.of(Currency.VND, new TransactionSummary.CurrencyTotal(0L, 30000L)), 1);
    given(botMessages.detailHeader("Hôm qua")).willReturn("📋 Hôm qua:");
    given(botMessages.detailLine(eq(1), eq("🍜"), eq("ăn sáng"), anyString(), anyString()))
        .willReturn("  1. 🍜 ăn sáng — Chi 30.000đ");
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.compactSeparator()).willReturn("──────");
    given(botMessages.summaryLine(anyString(), anyString(), eq("VND")))
        .willReturn("  Chi 30.000đ | Thu 0đ (VND)");
    given(botMessages.summaryTotal(1)).willReturn("Tổng: 1 giao dịch.");

    var result =
        renderer.render(RANGE, ViewMode.COMPACT, List.of(tx), summary, FlowFilter.ALL);

    assertThat(result)
        .contains("📋 Hôm qua:")
        .contains("🍜 ăn sáng")
        .contains("──────")
        .contains("Chi 30.000đ")
        .contains("Tổng: 1 giao dịch.");
  }

  private Transaction buildTransaction(
      TransactionType type, long amount, String note, Category category) {
    return Transaction.builder()
        .type(type)
        .amount(amount)
        .currency(Currency.VND)
        .category(category)
        .note(note)
        .occurredAt(Instant.parse("2026-05-26T10:00:00Z"))
        .build();
  }
}
