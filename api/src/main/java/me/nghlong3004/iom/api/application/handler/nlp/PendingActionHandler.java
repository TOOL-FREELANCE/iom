package me.nghlong3004.iom.api.application.handler.nlp;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.stereotype.Component;

/**
 * Handles confirmation/cancellation replies for pending transaction management actions.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 6/1/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingActionHandler {

  private static final Set<String> CONFIRM_KEYWORDS =
      Set.of("ok", "co", "có", "dong y", "đồng ý", "uh", "ừ", "um", "oki", "okay", "oke");
  private static final Set<String> CANCEL_KEYWORDS =
      Set.of("huy", "hủy", "khong", "không", "thoi", "thôi");

  private final ConversationContextStore contextStore;
  private final UserResolver userResolver;
  private final TransactionService transactionService;
  private final MessageSender messageSender;
  private final BotMessages botMessages;

  /**
   * Returns true if a pending confirmation/cancellation message was handled. If the conversation is
   * awaiting confirmation but the message is not a keyword, the pending action is cleared and false
   * is returned so the new message can be processed normally.
   */
  public boolean handleIfPending(IncomingMessage message, ConversationContext context) {
    if (!context.isAwaitingConfirmation()) {
      return false;
    }

    var text = message.normalizedText().toLowerCase();
    if (CONFIRM_KEYWORDS.contains(text)) {
      executeConfirm(message, context);
      return true;
    }

    if (CANCEL_KEYWORDS.contains(text)) {
      context.clearPending();
      contextStore.save(context);
      messageSender.send(OutgoingMessage.replyTo(message, botMessages.manageCancelled()));
      return true;
    }

    context.clearPending();
    contextStore.save(context);
    return false;
  }

  private void executeConfirm(IncomingMessage message, ConversationContext context) {
    var pending = context.getPendingAction();
    if (pending == null) {
      context.clearPending();
      contextStore.save(context);
      return;
    }

    var user = userResolver.resolve(message);
    String reply;

    try {
      switch (pending.actionType()) {
        case DELETE -> {
          transactionService.delete(user, pending.transactionId());
          var desc =
              pending.description() != null && !pending.description().isBlank()
                  ? pending.description()
                  : String.valueOf(pending.transactionId());
          reply = botMessages.manageDeleted(desc);
          log.info("Confirmed DELETE txId={}", pending.transactionId());
        }
        default -> reply = botMessages.fallbackMessage();
      }
    } catch (RuntimeException exception) {
      log.warn("Failed to execute pending action: {}", exception.toString());
      reply = botMessages.manageNotFound();
    }

    context.clearPending();
    contextStore.save(context);
    messageSender.send(OutgoingMessage.replyTo(message, reply));
  }
}

