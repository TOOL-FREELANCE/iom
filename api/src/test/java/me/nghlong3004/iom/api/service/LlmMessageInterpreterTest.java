package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LlmMessageInterpreter Unit Tests")
class LlmMessageInterpreterTest {

  @Mock private ChatModel chatModel;

  private LlmMessageInterpreter interpreter;

  @BeforeEach
  void setUp() {
    interpreter =
        new LlmMessageInterpreter(
          JsonMapper.builder()
              .findAndAddModules()
              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .build(),
          chatModel);
  }

  @Test
  @DisplayName("Should return empty and skip model call when input is blank")
  void interpret_BlankInput_ReturnsEmpty() {
    var result = interpreter.interpret("   ");

    assertThat(result).isEmpty();
    verify(chatModel, never()).call(any(SystemMessage.class), any(UserMessage.class));
  }

  @Test
  @DisplayName("Should return empty when model says message is not a transaction")
  void interpret_NonTransactionJson_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn("{\"is_transaction\": false}");

    var result = interpreter.interpret("hello");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should parse valid expense JSON into ParsedTransaction")
  void interpret_ValidExpenseJson_ReturnsParsedTransaction() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_transaction": true, "type": "EXPENSE", "amount": 30000, "currency": "VND", "category": "FOOD", "note": "an sang", "occurred_at": "2026-05-26"}
            """);

    var result = interpreter.interpret("an sang 30k");

    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            parsed -> {
              assertThat(parsed.type()).isEqualTo(TransactionType.EXPENSE);
              assertThat(parsed.amount()).isEqualTo(30000L);
              assertThat(parsed.currency()).isEqualTo(Currency.VND);
              assertThat(parsed.category()).isEqualTo(Category.FOOD);
              assertThat(parsed.note()).isEqualTo("an sang");
              assertThat(parsed.occurredAt()).hasToString("2026-05-26");
            });
  }

  @Test
  @DisplayName("Should parse valid income JSON with null occurred date")
  void interpret_ValidIncomeJson_ReturnsParsedTransaction() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_transaction": true, "type": "INCOME", "amount": 5000000, "currency": "VND", "category": "SALARY", "note": "luong", "occurred_at": null}
            """);

    var result = interpreter.interpret("luong 5tr");

    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            parsed -> {
              assertThat(parsed.type()).isEqualTo(TransactionType.INCOME);
              assertThat(parsed.amount()).isEqualTo(5000000L);
              assertThat(parsed.category()).isEqualTo(Category.SALARY);
              assertThat(parsed.occurredAt()).isNull();
            });
  }

  @Test
  @DisplayName("Should return empty when model returns malformed JSON")
  void interpret_MalformedJson_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class))).willReturn("{bad-json");

    var result = interpreter.interpret("an sang 30k");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when model returns invalid amount")
  void interpret_InvalidAmount_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_transaction": true, "type": "EXPENSE", "amount": 0, "currency": "VND", "category": "FOOD", "note": "an sang", "occurred_at": null}
            """);

    var result = interpreter.interpret("an sang 0d");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when model call fails")
  void interpret_ModelException_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willThrow(new IllegalStateException("DeepSeek unavailable"));

    var result = interpreter.interpret("an sang 30k");

    assertThat(result).isEmpty();
  }
}
