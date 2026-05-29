package me.nghlong3004.iom.api.application.handler.nlp;

import java.text.Normalizer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.service.DateRangeResolverChain;
import me.nghlong3004.iom.api.service.LlmDateRangeResolver;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles natural-language finance view requests using a pipeline architecture:
 *
 * <ol>
 *   <li>{@link DateRangeResolverChain} — resolves date range (keyword first, LLM fallback)
 *   <li>Detect {@link FlowFilter} and {@link ViewMode} from keywords
 *   <li>Fetch data via {@link TransactionService}
 *   <li>Render via {@link FinanceViewRenderer}
 * </ol>
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Component
@Order(80)
@RequiredArgsConstructor
public class ViewFinancesHandler implements BotMessageHandler {

  private static final int COMPACT_THRESHOLD = 10;

  private final DateRangeResolverChain dateRangeChain;
  private final FinanceViewRenderer renderer;
  private final TransactionService transactionService;
  private final UserResolver userResolver;
  private final MessageSender messageSender;
  private final BotIntentProperties botIntentProperties;
  private final ConversationContextStore contextStore;

  @Override
  public boolean supports(IncomingMessage message) {
    return !message.isCommand() && message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var normalizedText = stripAccents(message.normalizedText());

    // Set ThreadLocal context for LLM clarification side-effect
    LlmDateRangeResolver.setCurrentMessage(message);
    try {
      var dateRange = dateRangeChain.resolve(message.normalizedText());
      if (dateRange.isEmpty()) {
        return false;
      }

      var flowFilter = detectFlowFilter(normalizedText);
      var viewMode = detectViewMode(normalizedText);
      var user = userResolver.resolve(message);
      var transactions = transactionService.findByRange(user, dateRange.get());

      // Save viewed transaction IDs for ByIndex reference (delete/update)
      if (!transactions.isEmpty()) {
        var context = contextStore.get(message.channel() + ":" + message.externalUserId());
        context.setLastViewedTransactionIds(
            transactions.stream().map(t -> t.getId()).toList());
        contextStore.save(context);
      }

      var summary = TransactionSummary.from(transactions);
      var effectiveMode = autoAdjustViewMode(viewMode, transactions.size());
      var reply =
          renderer.render(dateRange.get(), effectiveMode, transactions, summary, flowFilter);
      messageSender.send(OutgoingMessage.replyTo(message, reply));
      return true;
    } finally {
      LlmDateRangeResolver.clearCurrentMessage();
    }
  }

  private FlowFilter detectFlowFilter(String text) {
    var summaryConfig = botIntentProperties.summary();
    var hasExpense = hasAny(text, summaryConfig.expenseKeywords());
    var hasIncome = hasAny(text, summaryConfig.incomeKeywords());
    if (hasExpense == hasIncome) {
      return FlowFilter.ALL;
    }
    return hasExpense ? FlowFilter.EXPENSE : FlowFilter.INCOME;
  }

  private ViewMode detectViewMode(String text) {
    return hasAny(text, botIntentProperties.summary().detailKeywords())
        ? ViewMode.DETAIL
        : ViewMode.SUMMARY;
  }

  private ViewMode autoAdjustViewMode(ViewMode requested, int transactionCount) {
    if (requested == ViewMode.DETAIL && transactionCount > COMPACT_THRESHOLD) {
      return ViewMode.SUMMARY;
    }
    if (requested == ViewMode.DETAIL && transactionCount > 0) {
      return ViewMode.COMPACT;
    }
    return requested;
  }

  private boolean hasAny(String text, Iterable<String> keywords) {
    if (keywords == null) {
      return false;
    }
    for (String keyword : keywords) {
      if (text.contains(stripAccents(keyword))) {
        return true;
      }
    }
    return false;
  }

  private String stripAccents(String text) {
    var decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    return decomposed
        .replaceAll("\\p{M}", "")
        .replace('\u0111', 'd')
        .replace('\u0110', 'D')
        .toLowerCase(Locale.ROOT)
        .trim();
  }
}
