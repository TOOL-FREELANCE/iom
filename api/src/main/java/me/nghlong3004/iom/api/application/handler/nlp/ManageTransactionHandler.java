package me.nghlong3004.iom.api.application.handler.nlp;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.AmountFormatter;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionAction;
import me.nghlong3004.iom.api.domain.transaction.TransactionReference;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.ActionResolverChain;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles transaction management operations: delete, update, undo, confirm, cancel.
 *
 * <p>Placed between {@link RecordTransactionHandler} (50) and {@link ViewFinancesHandler} (80). If
 * the context is awaiting confirmation, this handler intercepts "ok"/"hủy" messages. Otherwise, it
 * attempts to parse management actions from the user's text.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class ManageTransactionHandler implements BotMessageHandler {

  private final ActionResolverChain actionResolverChain;
  private final ConversationContextStore contextStore;
  private final TransactionService transactionService;
  private final UserResolver userResolver;
  private final MessageSender messageSender;
  private final BotMessages botMessages;

  @Override
  public boolean supports(IncomingMessage message) {
    return !message.isCommand() && message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var context = contextStore.get(contextKey(message));

    // 1. If awaiting confirmation, intercept confirm/cancel
    if (context.isAwaitingConfirmation()) {
      return handleConfirmationResponse(message, context);
    }

    // 2. Try to parse management action
    var action = actionResolverChain.resolve(message.normalizedText());
    return action
        .map(
            transactionAction ->
                switch (transactionAction) {
                  case TransactionAction.Delete delete -> handleDelete(delete, message, context);
                  case TransactionAction.Update update -> handleUpdate(update, message, context);
                  case TransactionAction.Undo undo -> handleUndo(message, context);
                  case TransactionAction.Confirm confirm -> false;
                  case TransactionAction.Cancel cancel -> false;
                })
        .orElse(false);
  }

  private boolean handleConfirmationResponse(IncomingMessage message, ConversationContext context) {
    var confirmOrCancel = actionResolverChain.resolve(message.normalizedText());
    return confirmOrCancel
        .map(
            transactionAction ->
                switch (transactionAction) {
                  case TransactionAction.Confirm confirm -> executeConfirm(message, context);
                  case TransactionAction.Cancel cancel -> executeCancel(message, context);
                  default -> false; // other actions don't apply during confirmation
                })
        .orElse(false);
  }

  private boolean handleDelete(
      TransactionAction.Delete delete, IncomingMessage message, ConversationContext context) {
    var user = userResolver.resolve(message);
    var tx = resolveTransaction(delete.reference(), user, context);

    if (tx.isEmpty()) {
      sendReferenceError(delete.reference(), message, context);
      return true;
    }

    context.setPending(delete, tx.get());
    contextStore.save(context);
    var desc = formatTransaction(tx.get());
    sendReply(message, botMessages.manageConfirmDelete(desc));
    return true;
  }

  private boolean handleUpdate(
      TransactionAction.Update update, IncomingMessage message, ConversationContext context) {
    var user = userResolver.resolve(message);
    var tx = resolveTransaction(update.reference(), user, context);

    if (tx.isEmpty()) {
      sendReferenceError(update.reference(), message, context);
      return true;
    }

    context.setPending(update, tx.get());
    contextStore.save(context);
    var currentDesc = formatTransaction(tx.get());
    var changeDesc = describeChanges(update);
    sendReply(message, botMessages.manageConfirmUpdate(currentDesc, changeDesc));
    return true;
  }

  private boolean handleUndo(IncomingMessage message, ConversationContext context) {
    if (!context.hasLastRecorded()) {
      sendReply(message, botMessages.manageNoRecent());
      return true;
    }

    var user = userResolver.resolve(message);
    var tx = transactionService.findByUserAndId(user, context.getLastRecordedTransactionId());

    if (tx.isEmpty()) {
      sendReply(message, botMessages.manageNotFound());
      return true;
    }

    var desc = formatTransaction(tx.get());
    transactionService.delete(user, tx.get().getId());
    context.setLastRecordedTransactionId(null);
    contextStore.save(context);
    sendReply(message, botMessages.manageUndone(desc));
    log.info("Undo: deleted transaction id={}", tx.get().getId());
    return true;
  }

  private boolean executeConfirm(IncomingMessage message, ConversationContext context) {
    var pending = context.getPendingAction();
    var target = context.getPendingTarget();
    var user = userResolver.resolve(message);

    context.clearPending();
    contextStore.save(context);

    return switch (pending) {
      case TransactionAction.Delete d -> {
        var desc = formatTransaction(target);
        transactionService.delete(user, target.getId());
        sendReply(message, botMessages.manageDeleted(desc));
        log.info("Confirmed delete: transaction id={}", target.getId());
        yield true;
      }
      case TransactionAction.Update u -> {
        var updated = transactionService.update(user, target.getId(), u.changes());
        sendReply(message, botMessages.manageUpdated(formatTransaction(updated)));
        log.info("Confirmed update: transaction id={}", target.getId());
        yield true;
      }
      default -> false;
    };
  }

  private boolean executeCancel(IncomingMessage message, ConversationContext context) {
    context.clearPending();
    contextStore.save(context);
    sendReply(message, botMessages.manageCancelled());
    return true;
  }

  private Optional<Transaction> resolveTransaction(
      TransactionReference reference, AppUser user, ConversationContext context) {
    return switch (reference) {
      case TransactionReference.Latest l -> {
        if (!context.hasLastRecorded()) {
          yield Optional.empty();
        }
        yield transactionService.findByUserAndId(user, context.getLastRecordedTransactionId());
      }
      case TransactionReference.ByIndex bi -> {
        var txId = context.resolveByIndex(bi.index());
        if (txId == null) {
          yield Optional.empty();
        }
        yield transactionService.findByUserAndId(user, txId);
      }
      case TransactionReference.ByMatch bm ->
          // ByMatch is resolved by LLM — for now, not found
          Optional.empty();
    };
  }

  private void sendReferenceError(
      TransactionReference reference, IncomingMessage message, ConversationContext context) {
    var reply =
        switch (reference) {
          case TransactionReference.Latest l -> botMessages.manageNoRecent();
          case TransactionReference.ByIndex bi ->
              context.hasViewedList() ? botMessages.manageNotFound() : botMessages.manageNoList();
          case TransactionReference.ByMatch bm -> botMessages.manageNotFound();
        };
    sendReply(message, reply);
  }

  private String formatTransaction(Transaction tx) {
    var isIncome = tx.getType() == TransactionType.INCOME;
    var typeLabel = botMessages.typeLabel(isIncome);
    var formattedAmount = AmountFormatter.format(tx.getAmount(), tx.getCurrency());
    var note = tx.getNote() != null ? tx.getNote() : "";
    return typeLabel + " " + formattedAmount + (note.isEmpty() ? "" : " cho " + note);
  }

  private String describeChanges(TransactionAction.Update update) {
    var changes = update.changes();
    var sb = new StringBuilder();
    if (changes.amount() != null) {
      sb.append("số tiền mới");
    }
    if (changes.category() != null) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append("danh mục: ").append(changes.category().name());
    }
    if (changes.note() != null) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append("ghi chú: ").append(changes.note());
    }
    return sb.isEmpty() ? "thay đổi" : sb.toString();
  }

  private void sendReply(IncomingMessage message, String text) {
    messageSender.send(OutgoingMessage.replyTo(message, text));
  }

  private String contextKey(IncomingMessage message) {
    return message.channel() + ":" + message.externalUserId();
  }
}
