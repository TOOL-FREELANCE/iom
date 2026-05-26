package me.nghlong3004.iom.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.port.out.MessageInterpreter;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * {@link MessageInterpreter} implementation backed by DeepSeek through Spring AI.
 *
 * <p>Returns {@link Optional#empty()} when the input is blank, the model says it is not a
 * transaction, the model call fails, or the model output cannot be parsed into a valid
 * {@link ParsedTransaction}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmMessageInterpreter implements MessageInterpreter {

  private static final String SYSTEM_PROMPT =
      """
      You are a financial transaction parser. Given a user message, extract structured data.
      Respond with one valid JSON object only. Do not use markdown or extra text.

      Rules:
      - If the message is NOT about a financial transaction, respond with: {"is_transaction": false}
      - If it IS a transaction, respond with JSON:
        {
          "is_transaction": true,
          "type": "INCOME" or "EXPENSE",
          "amount": <number in smallest currency unit>,
          "currency": "VND" or "USD" or "EUR" or "JPY" or "KRW" or "GBP",
          "category": "FOOD" or "TRANSPORT" or "SALARY" or "EDUCATION" or "SHOPPING" or "ENTERTAINMENT" or "HEALTH" or "HOUSING" or "OTHER",
          "note": "<descriptive note extracted from the message>",
          "occurred_at": "<ISO date yyyy-MM-dd or null for today>"
        }

      Currency detection:
      - Vietnamese amount words with or without accents: k, nghin, trieu, dong, d -> VND
      - No currency indicator -> VND
      - "$", "dollar", "usd" -> USD
      - "euro", "eur" -> EUR
      - "yen", "jpy" -> JPY
      - "won", "krw" -> KRW
      - "pound", "gbp" -> GBP

      Amount conversion to smallest unit:
      - VND: "30k" = 30000, "5tr" = 5000000, "50 nghin" = 50000
      - USD/EUR/GBP: "$10.50" = 1050 (cents), "$5" = 500
      - JPY/KRW: store as-is (no fractional units)

      Type detection:
      - Income keywords: luong, thuong, nhan, thu, duoc cho, tien thuong, freelance, salary, bonus, income, received
      - Default: EXPENSE

      Date detection:
      - "hom qua" / "yesterday" -> yesterday's date
      - "hom kia" -> day before yesterday
      - Default: today

      Example input: an sang 30k
      Example output: {"is_transaction": true, "type": "EXPENSE", "amount": 30000, "currency": "VND", "category": "FOOD", "note": "an sang", "occurred_at": null}
      """;

  private final ObjectMapper objectMapper;
  private final ChatModel chatModel;

  @Override
  public Optional<ParsedTransaction> interpret(String text) {
    if (text == null || text.isBlank()) {
      return Optional.empty();
    }

    var normalizedText = text.trim();

    try {
      var content =
          chatModel.call(
              new SystemMessage(SYSTEM_PROMPT),
              new UserMessage(buildUserPrompt(normalizedText)));

      return parseResponse(content);
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to interpret message with DeepSeek. text={}, reason={}",
          normalizedText,
          exception.toString());
      return Optional.empty();
    }
  }

  private String buildUserPrompt(String text) {
    return "Current date: " + LocalDate.now() + "\nUser message: " + text;
  }

  private Optional<ParsedTransaction> parseResponse(String content) {
    if (content == null || content.isBlank()) {
      log.warn("DeepSeek returned empty content.");
      return Optional.empty();
    }

    try {
      var response = objectMapper.readValue(content, LlmTransactionResponse.class);
      if (!Boolean.TRUE.equals(response.isTransaction())) {
        return Optional.empty();
      }

      return Optional.of(
          new ParsedTransaction(
              response.type(),
              response.amount(),
              response.currency(),
              response.category(),
              response.note(),
              response.occurredAt()));
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      log.warn(
          "Failed to parse DeepSeek response. content={}, reason={}",
          content,
          exception.toString());
      return Optional.empty();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record LlmTransactionResponse(
      @JsonProperty("is_transaction") Boolean isTransaction,
      TransactionType type,
      long amount,
      Currency currency,
      Category category,
      String note,
      @JsonProperty("occurred_at") LocalDate occurredAt) {}
}
