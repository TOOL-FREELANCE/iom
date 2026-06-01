package me.nghlong3004.iom.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BotMessages Unit Tests")
class BotMessagesTest {

  @Mock private MessageSource messageSource;

  @InjectMocks private BotMessages botMessages;

  private static final java.util.Locale VI_LOCALE = java.util.Locale.of("vi", "VN");

  @Test
  @DisplayName("transactionRecorded should use note template when note is present")
  void transactionRecorded_WithNote_UsesNoteTemplate() {
    given(messageSource.getMessage("bot.transaction.recorded", new Object[] {"Chi", "30.000đ", "an sang"}, VI_LOCALE))
        .willReturn("Da ghi nhan: Chi 30.000đ cho an sang.");

    var result = botMessages.transactionRecorded("Chi", "30.000đ", "an sang");

    assertThat(result).isEqualTo("Da ghi nhan: Chi 30.000đ cho an sang.");
  }

  @Test
  @DisplayName("transactionRecorded should use no-note template when note is blank")
  void transactionRecorded_BlankNote_UsesNoNoteTemplate() {
    given(messageSource.getMessage("bot.transaction.recorded.no-note", new Object[] {"Thu", "5.000.000đ"}, VI_LOCALE))
        .willReturn("Da ghi nhan: Thu 5.000.000đ.");

    var result = botMessages.transactionRecorded("Thu", "5.000.000đ", "");

    assertThat(result).isEqualTo("Da ghi nhan: Thu 5.000.000đ.");
  }

  @Test
  @DisplayName("transactionRecorded should use no-note template when note is null")
  void transactionRecorded_NullNote_UsesNoNoteTemplate() {
    given(messageSource.getMessage("bot.transaction.recorded.no-note", new Object[] {"Thu", "5.000.000đ"}, VI_LOCALE))
        .willReturn("Da ghi nhan: Thu 5.000.000đ.");

    var result = botMessages.transactionRecorded("Thu", "5.000.000đ", null);

    assertThat(result).isEqualTo("Da ghi nhan: Thu 5.000.000đ.");
  }

  @Test
  @DisplayName("typeLabel should return income label when true")
  void typeLabel_Income_ReturnsIncomeLabel() {
    given(messageSource.getMessage("bot.transaction.type.income", new Object[]{}, VI_LOCALE))
        .willReturn("Thu");

    var result = botMessages.typeLabel(true);

    assertThat(result).isEqualTo("Thu");
  }

  @Test
  @DisplayName("typeLabel should return expense label when false")
  void typeLabel_Expense_ReturnsExpenseLabel() {
    given(messageSource.getMessage("bot.transaction.type.expense", new Object[]{}, VI_LOCALE))
        .willReturn("Chi");

    var result = botMessages.typeLabel(false);

    assertThat(result).isEqualTo("Chi");
  }
}
