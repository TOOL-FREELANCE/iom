package me.nghlong3004.iom.api.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;

/**
 * Immutable summary of transactions grouped by currency within a date range.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public record TransactionSummary(Map<Currency, CurrencyTotal> totals, int transactionCount) {

  public record CurrencyTotal(long totalIncome, long totalExpense) {}

  public static TransactionSummary from(List<Transaction> transactions) {
    var grouped =
        transactions.stream()
            .collect(
                Collectors.groupingBy(
                    Transaction::getCurrency,
                    Collectors.teeing(
                        Collectors.filtering(
                            transaction -> transaction.getType() == TransactionType.INCOME,
                            Collectors.summingLong(Transaction::getAmount)),
                        Collectors.filtering(
                            transaction -> transaction.getType() == TransactionType.EXPENSE,
                            Collectors.summingLong(Transaction::getAmount)),
                        CurrencyTotal::new)));

    return new TransactionSummary(grouped, transactions.size());
  }
}
