package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("LlmSummaryIntentInterpreter Unit Tests")
class LlmSummaryIntentInterpreterTest {

  @Mock private ChatModel chatModel;

  private LlmSummaryIntentInterpreter interpreter;

  @BeforeEach
  void setUp() {
    interpreter =
        new LlmSummaryIntentInterpreter(
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
  @DisplayName("Should parse valid yesterday expense summary JSON")
  void interpret_ValidYesterdayExpenseJson_ReturnsParsedIntent() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_summary": true, "needs_clarification": false, "from": "2026-05-25", "to": "2026-05-26", "label": "Hom qua", "flow_filter": "EXPENSE", "clarification_message": null}
            """);

    var result = interpreter.interpret("hom qua tieu bao nhieu");

    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            intent -> {
              assertThat(intent.needsClarification()).isFalse();
              assertThat(intent.from()).isEqualTo(startOfDay("2026-05-25"));
              assertThat(intent.to()).isEqualTo(startOfDay("2026-05-26"));
              assertThat(intent.label()).isEqualTo("Hom qua");
              assertThat(intent.flowFilter()).isEqualTo(FlowFilter.EXPENSE);
            });
  }

  @Test
  @DisplayName("Should parse valid custom date range JSON")
  void interpret_ValidCustomRangeJson_ReturnsParsedIntent() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_summary": true, "needs_clarification": false, "from": "2026-05-01", "to": "2026-05-21", "label": "Tu 1/5 den 20/5", "flow_filter": "ALL"}
            """);

    var result = interpreter.interpret("tu 1/5 den 20/5 chi the nao");

    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            intent -> {
              assertThat(intent.from()).isEqualTo(startOfDay("2026-05-01"));
              assertThat(intent.to()).isEqualTo(startOfDay("2026-05-21"));
              assertThat(intent.label()).isEqualTo("Tu 1/5 den 20/5");
              assertThat(intent.flowFilter()).isEqualTo(FlowFilter.ALL);
            });
  }

  @Test
  @DisplayName("Should return empty when model says message is not a summary")
  void interpret_NonSummaryJson_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn("{\"is_summary\": false}");

    var result = interpreter.interpret("hello");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should parse clarification JSON")
  void interpret_ClarificationJson_ReturnsClarificationIntent() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_summary": true, "needs_clarification": true, "clarification_message": "Ban muon xem khoang ngay nao?"}
            """);

    var result = interpreter.interpret("may hom truoc thi sao");

    assertThat(result)
        .isPresent()
        .get()
        .satisfies(
            intent -> {
              assertThat(intent.needsClarification()).isTrue();
              assertThat(intent.clarificationMessage()).isEqualTo("Ban muon xem khoang ngay nao?");
            });
  }

  @Test
  @DisplayName("Should return empty when model returns malformed JSON")
  void interpret_MalformedJson_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class))).willReturn("{bad-json");

    var result = interpreter.interpret("hom qua tieu bao nhieu");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when model returns invalid dates")
  void interpret_InvalidDate_ReturnsEmpty() {
    given(chatModel.call(any(SystemMessage.class), any(UserMessage.class)))
        .willReturn(
            """
            {"is_summary": true, "needs_clarification": false, "from": "2026-13-01", "to": "2026-05-21", "label": "Bad", "flow_filter": "EXPENSE"}
            """);

    var result = interpreter.interpret("hom qua tieu bao nhieu");

    assertThat(result).isEmpty();
  }

  private java.time.Instant startOfDay(String date) {
    return LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
}
