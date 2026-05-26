package me.nghlong3004.iom.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmationFormatter Unit Tests")
class ConfirmationFormatterTest {

  @Mock private BotMessages botMessages;

  @InjectMocks private ConfirmationFormatter formatter;

  @Test
  @DisplayName("Should format expense confirmation with note")
  void format_ExpenseWithNote_ReturnsConfirmation() {
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE, 30000L, Currency.VND, Category.FOOD, "an sang", null);
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.transactionRecorded("Chi", "30.000d", "an sang"))
        .willReturn("Da ghi nhan: Chi 30.000d cho an sang.");

    var result = formatter.format(parsed);

    assertThat(result).isEqualTo("Da ghi nhan: Chi 30.000d cho an sang.");
  }

  @Test
  @DisplayName("Should format income confirmation without note")
  void format_IncomeWithoutNote_ReturnsConfirmation() {
    var parsed =
        new ParsedTransaction(TransactionType.INCOME, 5000000L, Currency.VND, Category.SALARY, "", null);
    given(botMessages.typeLabel(true)).willReturn("Thu");
    given(botMessages.transactionRecorded("Thu", "5.000.000d", ""))
        .willReturn("Da ghi nhan: Thu 5.000.000d.");

    var result = formatter.format(parsed);

    assertThat(result).isEqualTo("Da ghi nhan: Thu 5.000.000d.");
  }
}
