package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("TransactionDraftMapper Unit Tests")
class TransactionDraftMapperTest {

  private final TransactionDraftMapper mapper = Mappers.getMapper(TransactionDraftMapper.class);

  @Test
  @DisplayName("Should map valid draft to parsed transaction")
  void toParsed_ValidDraft_MapsFields() {
    var draft = new TransactionDraft("expense", 30000L, "vnd", "food", "ăn sáng", "2026-05-28");

    var result = mapper.toParsed(draft);

    assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(result.amount()).isEqualTo(30000L);
    assertThat(result.currency()).isEqualTo(Currency.VND);
    assertThat(result.category()).isEqualTo(Category.FOOD);
    assertThat(result.note()).isEqualTo("ăn sáng");
    assertThat(result.occurredAt()).isEqualTo(LocalDate.of(2026, 5, 28));
  }

  @Test
  @DisplayName("Should map null date to null occurredAt")
  void toParsed_NullDate_ReturnsNullOccurredAt() {
    var draft = new TransactionDraft("INCOME", 5000000L, "VND", "SALARY", "lương", null);

    var result = mapper.toParsed(draft);

    assertThat(result.occurredAt()).isNull();
  }

  @Test
  @DisplayName("Should reject invalid enum")
  void toParsed_InvalidEnum_ThrowsException() {
    var draft = new TransactionDraft("BAD", 30000L, "VND", "FOOD", "ăn sáng", null);

    assertThatThrownBy(() -> mapper.toParsed(draft))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should reject invalid amount")
  void toParsed_InvalidAmount_ThrowsException() {
    var draft = new TransactionDraft("EXPENSE", 0L, "VND", "FOOD", "ăn sáng", null);

    assertThatThrownBy(() -> mapper.toParsed(draft))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must be positive");
  }
}
