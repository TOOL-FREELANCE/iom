package me.nghlong3004.iom.api.common;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.stereotype.Component;

/**
 * Formats transaction summaries for bot replies.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@Component
@RequiredArgsConstructor
public class SummaryFormatter {

  private final BotMessages botMessages;

  public String format(String label, TransactionSummary summary) {
    return format(label, summary, FlowFilter.ALL);
  }

  public String format(String label, TransactionSummary summary, FlowFilter flowFilter) {
    if (summary.transactionCount() == 0) {
      return botMessages.summaryEmpty(label);
    }

    var filter = flowFilter == null ? FlowFilter.ALL : flowFilter;
    var sb = new StringBuilder(label).append(":\n");
    summary
        .totals()
        .forEach(
            (currency, total) -> {
              var income = AmountFormatter.format(total.totalIncome(), currency);
              var expense = AmountFormatter.format(total.totalExpense(), currency);
              sb.append(formatLine(filter, expense, income, currency.name())).append("\n");
            });
    sb.append(botMessages.summaryTotal(summary.transactionCount()));
    return sb.toString();
  }

  private String formatLine(FlowFilter flowFilter, String expense, String income, String currencyName) {
    return switch (flowFilter) {
      case EXPENSE -> botMessages.summaryExpenseLine(expense, currencyName);
      case INCOME -> botMessages.summaryIncomeLine(income, currencyName);
      case ALL -> botMessages.summaryLine(expense, income, currencyName);
    };
  }
}
