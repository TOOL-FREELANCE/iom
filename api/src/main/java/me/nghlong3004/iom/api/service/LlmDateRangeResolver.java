package me.nghlong3004.iom.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.port.out.DateRangeResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * LLM-backed {@link DateRangeResolver} that parses natural-language date expressions using DeepSeek
 * through Spring AI. Runs as fallback in the chain (after keyword resolver).
 *
 * <p>When the LLM indicates clarification is needed, a clarification reply is sent as a
 * side-effect and {@link Optional#empty()} is returned.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class LlmDateRangeResolver implements DateRangeResolver {

  private static final String SYSTEM_PROMPT =
      """
      You are a Vietnamese financial summary intent parser.
      Respond with one valid JSON object only. Do not use markdown or extra text.

      Task:
      - Decide whether the user asks to view transaction totals, summaries, or history.
      - Parse the date range when possible.
      - Parse whether the user asks for expenses, income, or both.

      Response schema:
      {
        "is_summary": true or false,
        "needs_clarification": true or false,
        "from": "yyyy-MM-dd or null",
        "to": "yyyy-MM-dd or null",
        "label": "short Vietnamese label or null",
        "flow_filter": "ALL" or "EXPENSE" or "INCOME" or null,
        "clarification_message": "Vietnamese clarification question or null"
      }

      Rules:
      - If the message is not about viewing totals, income, expenses, spending, history,
        or summaries, return {"is_summary": false}.
      - If it is a summary request but the period is missing or ambiguous, return
        {"is_summary": true, "needs_clarification": true, "clarification_message": "..."}.
      - If it is valid, return "needs_clarification": false and include from, to, label, flow_filter.
      - "from" is inclusive at start of day. "to" is exclusive at start of day.
      - Use the current date from the user prompt to resolve relative dates.
      - For a range "from 1/5 to 20/5", set from to the current-year 05-01 and to to 05-21.
      - "hom qua" means yesterday. "hom kia" with or without Vietnamese accents means two days ago.
      - "7 ngay qua" means the 7-day window ending tomorrow at start of day.
      - Expense words include chi, tieu, xai, spent, expense.
      - Income words include thu, nhan, luong, income, received.
      - Use "ALL" when the user asks for totals without specifying income or expense.
      """;

  private final ObjectMapper objectMapper;
  private final ChatModel chatModel;
  private final MessageSender messageSender;
  private final BotMessages botMessages;

  /** Thread-local context for sending clarification replies. */
  private static final ThreadLocal<IncomingMessage> currentMessage = new ThreadLocal<>();

  public static void setCurrentMessage(IncomingMessage message) {
    currentMessage.set(message);
  }

  public static void clearCurrentMessage() {
    currentMessage.remove();
  }

  @Override
  public Optional<DateRange> resolve(String normalizedText) {
    if (normalizedText == null || normalizedText.isBlank()) {
      return Optional.empty();
    }

    try {
      var content =
          chatModel.call(
              new SystemMessage(SYSTEM_PROMPT),
              new UserMessage(buildUserPrompt(normalizedText)));

      return parseResponse(content);
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to interpret with LLM. text={}, reason={}",
          normalizedText,
          exception.toString());
      return Optional.empty();
    }
  }

  private String buildUserPrompt(String text) {
    return "Current date: " + LocalDate.now() + "\nUser message: " + text;
  }

  private Optional<DateRange> parseResponse(String content) {
    if (content == null || content.isBlank()) {
      log.warn("LLM returned empty content.");
      return Optional.empty();
    }

    try {
      var response = objectMapper.readValue(content, LlmDateResponse.class);
      if (!Boolean.TRUE.equals(response.isSummary())) {
        return Optional.empty();
      }

      if (Boolean.TRUE.equals(response.needsClarification())) {
        sendClarificationReply(response.clarificationMessage());
        return Optional.empty();
      }

      var zone = ZoneId.systemDefault();
      var from = LocalDate.parse(response.from()).atStartOfDay(zone).toInstant();
      var to = LocalDate.parse(response.to()).atStartOfDay(zone).toInstant();
      return Optional.of(DateRange.custom(from, to, response.label()));
    } catch (JsonProcessingException | RuntimeException exception) {
      log.warn(
          "Failed to parse LLM date response. content={}, reason={}",
          content,
          exception.toString());
      return Optional.empty();
    }
  }

  private void sendClarificationReply(String clarificationMessage) {
    var message = currentMessage.get();
    if (message != null && clarificationMessage != null) {
      var reply = botMessages.summaryClarification(clarificationMessage);
      messageSender.send(OutgoingMessage.replyTo(message, reply));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record LlmDateResponse(
      @JsonProperty("is_summary") Boolean isSummary,
      @JsonProperty("needs_clarification") Boolean needsClarification,
      String from,
      String to,
      String label,
      @JsonProperty("flow_filter") FlowFilter flowFilter,
      @JsonProperty("clarification_message") String clarificationMessage) {}
}
