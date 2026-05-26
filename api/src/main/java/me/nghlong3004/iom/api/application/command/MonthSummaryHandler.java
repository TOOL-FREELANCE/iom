package me.nghlong3004.iom.api.application.command;

import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.common.SummaryFormatter;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code /month} command. Returns a summary of the current month's transactions grouped
 * by currency.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class MonthSummaryHandler implements BotCommandHandler {

  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final BotMessages botMessages;
  private final SummaryFormatter summaryFormatter;

  @Override
  public boolean supports(IncomingMessage message) {
    return BotCommandParser.matches(message, BotCommand.MONTH);
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var user = userResolver.resolve(message);
    var zone = ZoneId.systemDefault();
    var currentMonth = YearMonth.now(zone);
    var from = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
    var to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

    var label =
        botMessages.monthLabel(currentMonth.getMonthValue(), currentMonth.getYear());
    var summary = transactionService.summarize(user, from, to);
    var reply = summaryFormatter.format(label, summary);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
    return true;
  }
}
