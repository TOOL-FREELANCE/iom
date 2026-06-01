package me.nghlong3004.iom.api.domain.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext.PendingActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConversationContext Unit Tests")
class ConversationContextTest {

  private ConversationContext context;

  @BeforeEach
  void setUp() {
    context = new ConversationContext("TELEGRAM:user-1");
  }

  @Test
  @DisplayName("Should start in IDLE state")
  void newContext_IsIdle() {
    assertThat(context.getState()).isEqualTo(ConversationContext.ConversationState.IDLE);
    assertThat(context.isAwaitingConfirmation()).isFalse();
  }

  @Test
  @DisplayName("Should track last recorded transaction IDs")
  void setLastRecordedTransactionIds_SetsValue() {
    context.setLastRecordedTransactionIds(List.of(10L, 20L));
    assertThat(context.getLastRecordedTransactionIds()).containsExactly(10L, 20L);
    assertThat(context.resolveLatestRecorded()).isEqualTo(20L);
    assertThat(context.hasLastRecorded()).isTrue();
  }

  @Test
  @DisplayName("Should keep recorded transaction IDs bounded")
  void setLastRecordedTransactionIds_BoundsList() {
    context.setLastRecordedTransactionIds(
        java.util.stream.LongStream.rangeClosed(1, 12).boxed().toList());

    assertThat(context.getLastRecordedTransactionIds())
        .containsExactly(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
  }

  @Test
  @DisplayName("Should track last viewed transaction IDs")
  void setLastViewedTransactionIds_SetsValue() {
    context.setLastViewedTransactionIds(List.of(1L, 2L, 3L));
    assertThat(context.getLastViewedTransactionIds()).containsExactly(1L, 2L, 3L);
    assertThat(context.hasViewedList()).isTrue();
  }

  @Test
  @DisplayName("Should keep viewed transaction IDs bounded")
  void setLastViewedTransactionIds_BoundsList() {
    context.setLastViewedTransactionIds(
        java.util.stream.LongStream.rangeClosed(1, 22).boxed().toList());

    assertThat(context.getLastViewedTransactionIds())
        .containsExactly(
            3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L,
            13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L);
  }

  @Test
  @DisplayName("Should resolve by 1-based index")
  void resolveByIndex_ValidIndex_ReturnsId() {
    context.setLastViewedTransactionIds(List.of(10L, 20L, 30L));
    assertThat(context.resolveByIndex(1)).isEqualTo(10L);
    assertThat(context.resolveByIndex(2)).isEqualTo(20L);
    assertThat(context.resolveByIndex(3)).isEqualTo(30L);
  }

  @Test
  @DisplayName("Should return null for out-of-range index")
  void resolveByIndex_OutOfRange_ReturnsNull() {
    context.setLastViewedTransactionIds(List.of(10L, 20L));
    assertThat(context.resolveByIndex(0)).isNull();
    assertThat(context.resolveByIndex(3)).isNull();
  }

  @Test
  @DisplayName("Should transition to AWAITING_CONFIRMATION on setPending")
  void setPending_TransitionsToAwaitingConfirmation() {
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");

    assertThat(context.isAwaitingConfirmation()).isTrue();
    assertThat(context.getPendingAction().actionType()).isEqualTo(PendingActionType.DELETE);
    assertThat(context.getPendingAction().transactionId()).isEqualTo(42L);
    assertThat(context.getPendingAction().description()).isEqualTo("ăn sáng");
  }

  @Test
  @DisplayName("Should return to IDLE on clearPending")
  void clearPending_ReturnsToIdle() {
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");
    context.clearPending();

    assertThat(context.isAwaitingConfirmation()).isFalse();
    assertThat(context.getPendingAction()).isNull();
  }

  @Test
  @DisplayName("Should handle null viewed list gracefully")
  void setLastViewedTransactionIds_Null_SetsEmpty() {
    context.setLastViewedTransactionIds(null);
    assertThat(context.hasViewedList()).isFalse();
  }

  @Test
  @DisplayName("Should update lastActivityAt on mutations")
  void mutations_UpdateLastActivityAt() {
    var before = context.getLastActivityAt();
    context.setLastRecordedTransactionIds(List.of(1L));
    assertThat(context.getLastActivityAt()).isAfterOrEqualTo(before);
  }
}
