package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext.PendingActionType;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PendingActionHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class PendingActionHandlerTest {

  @Mock private ConversationContextStore contextStore;
  @Mock private UserResolver userResolver;
  @Mock private TransactionService transactionService;
  @Mock private MessageSender messageSender;
  @Mock private BotMessages botMessages;

  private PendingActionHandler handler;
  private ConversationContext context;

  @BeforeEach
  void setUp() {
    handler =
        new PendingActionHandler(
            contextStore, userResolver, transactionService, messageSender, botMessages);
    context = new ConversationContext("TELEGRAM:user1");
  }

  @Test
  @DisplayName("Should handle ok confirmation and execute pending delete")
  void handleIfPending_ConfirmDelete_ExecutesAndReplies() {
    var user = mock(AppUser.class);
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");
    when(userResolver.resolve(any())).thenReturn(user);
    when(botMessages.manageDeleted("ăn sáng")).thenReturn("Đã xoá giao dịch ăn sáng.");

    var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "ok");
    var result = handler.handleIfPending(message, context);

    assertThat(result).isTrue();
    verify(transactionService).delete(user, 42L);
    verify(messageSender).send(any(OutgoingMessage.class));
    assertThat(context.isAwaitingConfirmation()).isFalse();
  }

  @Test
  @DisplayName("Should handle cancellation")
  void handleIfPending_Cancel_ClearsPendingAndReplies() {
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");
    when(botMessages.manageCancelled()).thenReturn("Đã huỷ.");

    var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "hủy");
    var result = handler.handleIfPending(message, context);

    assertThat(result).isTrue();
    verify(transactionService, never()).delete(any(), any());
    var captor = ArgumentCaptor.forClass(OutgoingMessage.class);
    verify(messageSender).send(captor.capture());
    assertThat(captor.getValue().text()).isEqualTo("Đã huỷ.");
    assertThat(context.isAwaitingConfirmation()).isFalse();
  }

  @Test
  @DisplayName("Should clear pending and return false for non-keyword")
  void handleIfPending_NonKeyword_ClearsPendingAndReturnsFalse() {
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");

    var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "ăn trưa 50k");
    var result = handler.handleIfPending(message, context);

    assertThat(result).isFalse();
    assertThat(context.isAwaitingConfirmation()).isFalse();
    verifyNoInteractions(transactionService, messageSender);
  }
}
