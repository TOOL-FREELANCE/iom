package me.nghlong3004.iom.api.common;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.springframework.stereotype.Component;

/**
 * Formats transaction confirmation messages for bot replies.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
@RequiredArgsConstructor
public class ConfirmationFormatter {

  private final BotMessages botMessages;

  public String format(ParsedTransaction parsed) {
    var isIncome = parsed.type() == TransactionType.INCOME;
    var typeLabel = botMessages.typeLabel(isIncome);
    var formattedAmount = AmountFormatter.format(parsed.amount(), parsed.currency());

    return botMessages.transactionRecorded(typeLabel, formattedAmount, parsed.note());
  }

  public String formatBatch(List<ParsedTransaction> parsedTransactions) {
    if (parsedTransactions.size() == 1) {
      return format(parsedTransactions.getFirst());
    }

    var sb = new StringBuilder(botMessages.transactionRecordedBatchHeader(parsedTransactions.size()));
    for (int i = 0; i < parsedTransactions.size(); i++) {
      var parsed = parsedTransactions.get(i);
      var isIncome = parsed.type() == TransactionType.INCOME;
      var typeLabel = botMessages.typeLabel(isIncome);
      var formattedAmount = AmountFormatter.format(parsed.amount(), parsed.currency());
      sb.append("\n")
          .append(
              botMessages.transactionRecordedBatchLine(
                  i + 1, typeLabel, formattedAmount, parsed.note()));
    }
    return sb.toString();
  }
}
