package me.nghlong3004.iom.api.application.handler.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.MessageInterpreter;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.ConfirmationFormatter;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles non-command text messages that contain financial data. Delegates parsing to {@link
 * MessageInterpreter} and persists via {@link TransactionService}.
 *
 * <p>Ordered at 50 — after slash-command handlers (1-10), before echo fallback (99). If the
 * interpreter returns empty, this handler exits silently, allowing the router to fall through to
 * {@link ManageTransactionHandler}, then {@link ViewFinancesHandler}, then {@link
 * EchoMessageHandler}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class RecordTransactionHandler implements BotMessageHandler {

  private final MessageInterpreter messageInterpreter;
  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final ConfirmationFormatter confirmationFormatter;
  private final ConversationContextStore contextStore;

  @Override
  public boolean supports(IncomingMessage message) {
    return !message.isCommand() && message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var parsed = messageInterpreter.interpret(message.normalizedText());

    if (parsed.isEmpty()) {
      return false;
    }

    var user = userResolver.resolve(message);
    var transaction =
        transactionService.record(user, parsed.get(), message.channel(), message.normalizedText());

    // Save last recorded transaction ID for undo/reference
    var context = contextStore.get(contextKey(message));
    context.setLastRecordedTransactionId(transaction.getId());
    contextStore.save(context);

    var confirmation = confirmationFormatter.format(parsed.get());
    messageSender.send(OutgoingMessage.replyTo(message, confirmation));

    log.info("Transaction {} recorded for user {}", transaction.getId(), user.getId());
    return true;
  }

  private String contextKey(IncomingMessage message) {
    return message.channel() + ":" + message.externalUserId();
  }
}
