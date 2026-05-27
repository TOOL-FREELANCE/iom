package me.nghlong3004.iom.api.application.command;

import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code /today} command. Returns a summary of today's transactions grouped by
 * currency.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class TodaySummaryHandler implements BotCommandHandler {

  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final FinanceViewRenderer renderer;

  @Override
  public boolean supports(IncomingMessage message) {
    return BotCommandParser.matches(message, BotCommand.TODAY);
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var user = userResolver.resolve(message);
    var dateRange = DateRange.today(ZoneId.systemDefault());
    var transactions = transactionService.findByRange(user, dateRange);
    var summary = TransactionSummary.from(transactions);
    var reply =
        renderer.render(dateRange, ViewMode.SUMMARY, transactions, summary, FlowFilter.ALL);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
    return true;
  }
}
