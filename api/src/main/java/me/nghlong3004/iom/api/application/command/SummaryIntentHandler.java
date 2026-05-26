package me.nghlong3004.iom.api.application.command;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.SummaryIntentInterpreter;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.common.SummaryFormatter;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ParsedSummaryIntent;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles natural-language summary requests without requiring slash commands.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@Component
@Order(80)
@RequiredArgsConstructor
public class SummaryIntentHandler implements BotCommandHandler {

  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final BotMessages botMessages;
  private final SummaryFormatter summaryFormatter;
  private final BotIntentProperties botIntentProperties;
  private final SummaryIntentInterpreter summaryIntentInterpreter;

  @Override
  public boolean supports(IncomingMessage message) {
    return !message.isCommand() && message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var normalizedText = normalizeText(message.normalizedText());
    var summaryConfig = botIntentProperties.summary();
    var hasAction = hasAny(normalizedText, summaryConfig.actionKeywords());
    var flowFilter = detectFlowFilter(normalizedText, summaryConfig);

    if (hasAction && hasAny(normalizedText, summaryConfig.todayKeywords())) {
      sendTodaySummary(message, flowFilter);
      return true;
    }

    if (hasAction && hasAny(normalizedText, summaryConfig.monthKeywords())) {
      sendMonthSummary(message, flowFilter);
      return true;
    }

    return summaryIntentInterpreter
        .interpret(message.normalizedText())
        .map(intent -> handleParsedIntent(message, intent))
        .orElse(false);
  }

  private boolean handleParsedIntent(IncomingMessage message, ParsedSummaryIntent intent) {
    if (intent.needsClarification()) {
      var reply = botMessages.summaryClarification(intent.clarificationMessage());
      messageSender.send(OutgoingMessage.replyTo(message, reply));
      return true;
    }

    var user = userResolver.resolve(message);
    var summary = transactionService.summarize(user, intent.from(), intent.to());
    var reply = summaryFormatter.format(intent.label(), summary, intent.flowFilter());
    messageSender.send(OutgoingMessage.replyTo(message, reply));
    return true;
  }

  private void sendTodaySummary(IncomingMessage message, FlowFilter flowFilter) {
    var user = userResolver.resolve(message);
    var zone = ZoneId.systemDefault();
    var today = LocalDate.now(zone);
    var from = today.atStartOfDay(zone).toInstant();
    var to = today.plusDays(1).atStartOfDay(zone).toInstant();
    var summary = transactionService.summarize(user, from, to);
    var reply = summaryFormatter.format(botMessages.todayLabel(), summary, flowFilter);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
  }

  private void sendMonthSummary(IncomingMessage message, FlowFilter flowFilter) {
    var user = userResolver.resolve(message);
    var zone = ZoneId.systemDefault();
    var currentMonth = YearMonth.now(zone);
    var from = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
    var to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
    var label = botMessages.monthLabel(currentMonth.getMonthValue(), currentMonth.getYear());
    var summary = transactionService.summarize(user, from, to);
    var reply = summaryFormatter.format(label, summary, flowFilter);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
  }

  private FlowFilter detectFlowFilter(String text, BotIntentProperties.Summary summaryConfig) {
    var hasExpense = hasAny(text, summaryConfig.expenseKeywords());
    var hasIncome = hasAny(text, summaryConfig.incomeKeywords());
    if (hasExpense == hasIncome) {
      return FlowFilter.ALL;
    }
    return hasExpense ? FlowFilter.EXPENSE : FlowFilter.INCOME;
  }

  private boolean hasAny(String text, Iterable<String> keywords) {
    for (String keyword : keywords) {
      if (text.contains(normalizeText(keyword))) {
        return true;
      }
    }
    return false;
  }

  private String normalizeText(String text) {
    var decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    return decomposed
        .replaceAll("\\p{M}", "")
        .replace('\u0111', 'd')
        .replace('\u0110', 'D')
        .toLowerCase(Locale.ROOT)
        .trim();
  }
}
