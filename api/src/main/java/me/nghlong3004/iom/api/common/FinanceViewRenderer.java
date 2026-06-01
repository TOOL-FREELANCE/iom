package me.nghlong3004.iom.api.common;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.stereotype.Component;

/**
 * Renders finance view responses for bot replies. Supports three view modes: summary (totals only),
 * detail (individual transactions), and compact (transactions + totals).
 *
 * <p>Replaces {@code SummaryFormatter} with multi-mode rendering capability.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Component
@RequiredArgsConstructor
public class FinanceViewRenderer {

  private final BotMessages botMessages;

  /**
   * Renders a finance view response using the specified mode.
   *
   * @param range the date range being viewed
   * @param viewMode the rendering mode
   * @param transactions the individual transactions (needed for DETAIL/COMPACT)
   * @param summary the aggregated summary (needed for SUMMARY/COMPACT)
   * @param filter the flow filter controlling which totals to show
   * @return the formatted reply text
   */
  public String render(
      DateRange range,
      ViewMode viewMode,
      List<Transaction> transactions,
      TransactionSummary summary,
      FlowFilter filter) {
    return switch (viewMode) {
      case SUMMARY -> renderSummary(range, summary, filter);
      case DETAIL -> renderDetail(range, transactions, filter);
      case COMPACT -> renderCompact(range, transactions, summary, filter);
    };
  }

  private String renderSummary(DateRange range, TransactionSummary summary, FlowFilter filter) {
    if (summary.transactionCount() == 0) {
      return botMessages.summaryEmpty(range.label());
    }

    var effectiveFilter = filter == null ? FlowFilter.ALL : filter;
    var sb = new StringBuilder(range.label()).append(":\n");
    summary
        .totals()
        .forEach((currency, total) -> appendSummaryLines(sb, effectiveFilter, currency, total));
    sb.append(botMessages.summaryTotal(summary.transactionCount()));
    return sb.toString();
  }

  private String renderDetail(
      DateRange range, List<Transaction> transactions, FlowFilter filter) {
    var filtered = filterTransactions(transactions, filter);
    if (filtered.isEmpty()) {
      return botMessages.detailEmpty(range.label());
    }

    var sb = new StringBuilder(renderTransactionLines(range, filtered));
    sb.append("\n");
    sb.append(botMessages.summaryTotal(filtered.size()));
    return sb.toString();
  }

  private String renderCompact(
      DateRange range,
      List<Transaction> transactions,
      TransactionSummary summary,
      FlowFilter filter) {
    var filtered = filterTransactions(transactions, filter);
    if (filtered.isEmpty()) {
      return botMessages.detailEmpty(range.label());
    }

    var sb = new StringBuilder(renderTransactionLines(range, filtered));
    sb.append("\n");

    var effectiveFilter = filter == null ? FlowFilter.ALL : filter;
    summary
        .totals()
        .forEach((currency, total) -> appendSummaryLines(sb, effectiveFilter, currency, total));
    sb.append(botMessages.summaryTotal(filtered.size()));
    return sb.toString();
  }

  private void appendSummaryLines(
      StringBuilder sb,
      FlowFilter flowFilter,
      Currency currency,
      TransactionSummary.CurrencyTotal total) {
    var expense = AmountFormatter.format(total.totalExpense(), currency);
    var income = AmountFormatter.format(total.totalIncome(), currency);

    switch (flowFilter) {
      case EXPENSE ->
          sb.append(botMessages.summaryExpenseLine(expense, currency.name())).append("\n");
      case INCOME ->
          sb.append(botMessages.summaryIncomeLine(income, currency.name())).append("\n");
      case ALL -> {
        if (total.totalExpense() > 0) {
          sb.append(botMessages.summaryExpenseLine(expense, currency.name())).append("\n");
        }
        if (total.totalIncome() > 0) {
          sb.append(botMessages.summaryIncomeLine(income, currency.name())).append("\n");
        }
      }
    }
  }

  private List<Transaction> filterTransactions(List<Transaction> transactions, FlowFilter filter) {
    if (filter == null || filter == FlowFilter.ALL) {
      return transactions;
    }
    var targetType =
        filter == FlowFilter.EXPENSE ? TransactionType.EXPENSE : TransactionType.INCOME;
    return transactions.stream().filter(tx -> tx.getType() == targetType).toList();
  }

  private String renderTransactionLines(DateRange range, List<Transaction> transactions) {
    var sb = new StringBuilder(botMessages.detailHeader(range.label())).append("\n");
    var showType = hasMixedTransactionTypes(transactions);
    for (int i = 0; i < transactions.size(); i++) {
      var tx = transactions.get(i);
      var note = tx.getNote() != null ? tx.getNote() : tx.getCategory().name().toLowerCase();
      var isIncome = tx.getType() == TransactionType.INCOME;
      var formatted = AmountFormatter.format(tx.getAmount(), tx.getCurrency());
      if (showType) {
        var typeLabel = botMessages.typeLabel(isIncome);
        sb.append(botMessages.detailLine(i + 1, note, typeLabel, formatted)).append("\n");
      } else {
        sb.append(botMessages.detailAmountLine(i + 1, note, formatted)).append("\n");
      }
    }
    return sb.toString();
  }

  private boolean hasMixedTransactionTypes(List<Transaction> transactions) {
    return transactions.stream().map(Transaction::getType).distinct().limit(2).count() > 1;
  }
}
