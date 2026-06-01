package me.nghlong3004.iom.api.domain.conversation;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Per-user conversation context for stateful interactions. Tracks the last recorded transaction,
 * last viewed transaction list, and any pending action awaiting confirmation.
 *
 * <p>Platform-agnostic — works identically for Telegram, Web, Zalo, etc.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
@Getter
public class ConversationContext {

  private static final int MAX_RECORDED_TRANSACTION_IDS = 10;
  private static final int MAX_VIEWED_TRANSACTION_IDS = 20;

  private final String conversationKey;
  private List<Long> lastRecordedTransactionIds;
  private List<Long> lastViewedTransactionIds;
  private PendingAction pendingAction;
  private ConversationState state;
  private Instant lastActivityAt;

  public ConversationContext(String conversationKey) {
    this.conversationKey = conversationKey;
    this.lastRecordedTransactionIds = Collections.emptyList();
    this.lastViewedTransactionIds = Collections.emptyList();
    this.state = ConversationState.IDLE;
    this.lastActivityAt = Instant.now();
  }

  public enum ConversationState {
    IDLE,
    AWAITING_CONFIRMATION
  }

  public enum PendingActionType {
    DELETE,
    UPDATE
  }

  /**
   * Lightweight pending action descriptor. Stores only a stable transaction ID and display
   * description, avoiding persistence entities in conversation state.
   *
   * @param actionType the type of action, e.g. DELETE or UPDATE
   * @param transactionId the ID of the target transaction
   * @param description display text used in confirmation and success replies
   */
  public record PendingAction(
      PendingActionType actionType, Long transactionId, String description) {}

  public void setLastRecordedTransactionIds(List<Long> transactionIds) {
    this.lastRecordedTransactionIds = boundedCopy(transactionIds, MAX_RECORDED_TRANSACTION_IDS);
    touch();
  }

  public void setLastRecordedTransactionId(Long transactionId) {
    setLastRecordedTransactionIds(transactionId == null ? null : List.of(transactionId));
  }

  public Long getLastRecordedTransactionId() {
    return resolveLatestRecorded();
  }

  public void setLastViewedTransactionIds(List<Long> transactionIds) {
    this.lastViewedTransactionIds = boundedCopy(transactionIds, MAX_VIEWED_TRANSACTION_IDS);
    touch();
  }

  /**
   * Sets a pending action that requires user confirmation.
   *
   * @param actionType the action type, e.g. DELETE or UPDATE
   * @param transactionId the ID of the target transaction
   * @param description display text used in confirmation and success replies
   */
  public void setPending(PendingActionType actionType, Long transactionId, String description) {
    this.pendingAction = new PendingAction(actionType, transactionId, description);
    this.state = ConversationState.AWAITING_CONFIRMATION;
    touch();
  }

  /** Clears any pending action and returns to IDLE state. */
  public void clearPending() {
    this.pendingAction = null;
    this.state = ConversationState.IDLE;
    touch();
  }

  public boolean isAwaitingConfirmation() {
    return state == ConversationState.AWAITING_CONFIRMATION;
  }

  /**
   * Resolves a transaction ID from the last viewed list by 1-based index.
   *
   * @param index 1-based index
   * @return the transaction ID, or null if out of range
   */
  public Long resolveByIndex(int index) {
    if (lastViewedTransactionIds == null || index < 1 || index > lastViewedTransactionIds.size()) {
      return null;
    }
    return lastViewedTransactionIds.get(index - 1);
  }

  public boolean hasLastRecorded() {
    return lastRecordedTransactionIds != null && !lastRecordedTransactionIds.isEmpty();
  }

  public boolean hasViewedList() {
    return lastViewedTransactionIds != null && !lastViewedTransactionIds.isEmpty();
  }

  public Long resolveLatestRecorded() {
    if (!hasLastRecorded()) {
      return null;
    }
    return lastRecordedTransactionIds.get(lastRecordedTransactionIds.size() - 1);
  }

  private List<Long> boundedCopy(List<Long> transactionIds, int maxSize) {
    if (transactionIds == null || transactionIds.isEmpty()) {
      return Collections.emptyList();
    }
    var fromIndex = Math.max(0, transactionIds.size() - maxSize);
    return List.copyOf(transactionIds.subList(fromIndex, transactionIds.size()));
  }

  private void touch() {
    this.lastActivityAt = Instant.now();
  }
}
