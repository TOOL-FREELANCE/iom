package me.nghlong3004.iom.api.application.handler.command;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles all slash commands that produce a financial summary view ({@code /today}, {@code /month}).
 * Replaces the separate {@code TodaySummaryHandler} and {@code MonthSummaryHandler} by delegating
 * date range creation to {@link BotCommand#createDateRange(ZoneId)}.
 *
 * <p>To add a new summary command (e.g. {@code /week}), add an enum constant to {@link BotCommand}
 * with the appropriate {@link me.nghlong3004.iom.api.domain.summary.DateRange} factory — no new
 * handler class needed.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class SummaryCommandHandler implements BotMessageHandler {

  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final FinanceViewRenderer renderer;

  @Override
  public boolean supports(IncomingMessage message) {
    return findSummaryCommand(message).isPresent();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var command = findSummaryCommand(message).orElseThrow();
    var user = userResolver.resolve(message);
    var dateRange = command.createDateRange(ZoneId.systemDefault());
    var transactions = transactionService.findByRange(user, dateRange);
    var summary = TransactionSummary.from(transactions);
    var reply =
        renderer.render(dateRange, ViewMode.SUMMARY, transactions, summary, FlowFilter.ALL);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
    return true;
  }

  private Optional<BotCommand> findSummaryCommand(IncomingMessage message) {
    return Arrays.stream(BotCommand.values())
        .filter(BotCommand::hasSummary)
        .filter(cmd -> BotCommandParser.matches(message, cmd))
        .findFirst();
  }
}
