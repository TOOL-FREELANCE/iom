package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionAction;
import me.nghlong3004.iom.api.domain.transaction.TransactionReference;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.ActionResolverChain;
import me.nghlong3004.iom.api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ManageTransactionHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class ManageTransactionHandlerTest {

  @Mock private ActionResolverChain actionResolverChain;
  @Mock private ConversationContextStore contextStore;
  @Mock private TransactionService transactionService;
  @Mock private UserResolver userResolver;
  @Mock private MessageSender messageSender;
  @Mock private BotMessages botMessages;

  private ManageTransactionHandler handler;
  private ConversationContext context;
  private final AppUser user = AppUser.builder().id(1L).build();

  private final IncomingMessage message =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "xoa cai vua roi");

  @BeforeEach
  void setUp() {
    handler =
        new ManageTransactionHandler(
            actionResolverChain, contextStore, transactionService, userResolver, messageSender,
            botMessages);
    context = new ConversationContext("TELEGRAM:u-1");
  }

  private void givenContext() {
    given(contextStore.get("TELEGRAM:u-1")).willReturn(context);
  }

  @Test
  @DisplayName("Should support non-command text messages")
  void supports_NonCommandText_ReturnsTrue() {
    assertThat(handler.supports(message)).isTrue();
  }

  @Test
  @DisplayName("Should not support command messages")
  void supports_Command_ReturnsFalse() {
    var cmd = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today");
    assertThat(handler.supports(cmd)).isFalse();
  }

  @Test
  @DisplayName("Should return false when no action resolved")
  void handle_NoAction_ReturnsFalse() {
    givenContext();
    given(actionResolverChain.resolve(any())).willReturn(Optional.empty());
    assertThat(handler.handle(message)).isFalse();
  }

  @Test
  @DisplayName("Should handle delete Latest with confirmation")
  void handle_DeleteLatest_AsksConfirmation() {
    givenContext();
    context.setLastRecordedTransactionId(42L);
    var action = new TransactionAction.Delete(new TransactionReference.Latest());
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(action));
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.findByUserAndId(user, 42L))
        .willReturn(Optional.of(buildTransaction(42L)));
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.manageConfirmDelete(anyString())).willReturn("Confirm delete?");

    var result = handler.handle(message);

    assertThat(result).isTrue();
    assertThat(context.isAwaitingConfirmation()).isTrue();
    then(messageSender).should().send(any(OutgoingMessage.class));
  }

  @Test
  @DisplayName("Should handle undo when last transaction exists")
  void handle_Undo_DeletesLastTransaction() {
    givenContext();
    context.setLastRecordedTransactionId(42L);
    var action = new TransactionAction.Undo();
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(action));
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.findByUserAndId(user, 42L))
        .willReturn(Optional.of(buildTransaction(42L)));
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.manageUndone(anyString())).willReturn("Undone");

    var result = handler.handle(message);

    assertThat(result).isTrue();
    then(transactionService).should().delete(user, 42L);
    then(messageSender).should().send(any(OutgoingMessage.class));
  }

  @Test
  @DisplayName("Should handle undo when no recent transaction")
  void handle_Undo_NoRecent_SendsError() {
    givenContext();
    var action = new TransactionAction.Undo();
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(action));
    given(botMessages.manageNoRecent()).willReturn("No recent");

    var result = handler.handle(message);

    assertThat(result).isTrue();
    then(messageSender).should().send(any(OutgoingMessage.class));
  }

  @Test
  @DisplayName("Should execute confirm when awaiting confirmation")
  void handle_Confirm_ExecutesPending() {
    givenContext();
    var tx = buildTransaction(42L);
    context.setPending(
        new TransactionAction.Delete(new TransactionReference.Latest()), tx);
    var confirmAction = new TransactionAction.Confirm();
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(confirmAction));
    given(userResolver.resolve(any())).willReturn(user);
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.manageDeleted(anyString())).willReturn("Deleted");

    var confirmMessage =
        new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "ok");
    var result = handler.handle(confirmMessage);

    assertThat(result).isTrue();
    then(transactionService).should().delete(user, 42L);
    assertThat(context.isAwaitingConfirmation()).isFalse();
  }

  @Test
  @DisplayName("Should execute cancel when awaiting confirmation")
  void handle_Cancel_ClearsPending() {
    givenContext();
    context.setPending(
        new TransactionAction.Delete(new TransactionReference.Latest()),
        buildTransaction(42L));
    var cancelAction = new TransactionAction.Cancel();
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(cancelAction));
    given(botMessages.manageCancelled()).willReturn("Cancelled");

    var cancelMessage =
        new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "huy");
    var result = handler.handle(cancelMessage);

    assertThat(result).isTrue();
    assertThat(context.isAwaitingConfirmation()).isFalse();
  }

  @Test
  @DisplayName("Should handle delete ByIndex from viewed list")
  void handle_DeleteByIndex_AsksConfirmation() {
    givenContext();
    context.setLastViewedTransactionIds(List.of(10L, 20L, 30L));
    var action = new TransactionAction.Delete(new TransactionReference.ByIndex(2));
    given(actionResolverChain.resolve(any())).willReturn(Optional.of(action));
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.findByUserAndId(user, 20L))
        .willReturn(Optional.of(buildTransaction(20L)));
    given(botMessages.typeLabel(false)).willReturn("Chi");
    given(botMessages.manageConfirmDelete(anyString())).willReturn("Confirm?");

    var result = handler.handle(message);

    assertThat(result).isTrue();
    assertThat(context.isAwaitingConfirmation()).isTrue();
  }

  private Transaction buildTransaction(Long id) {
    return Transaction.builder()
        .id(id)
        .type(TransactionType.EXPENSE)
        .amount(30000L)
        .currency(Currency.VND)
        .category(Category.FOOD)
        .note("ăn sáng")
        .occurredAt(Instant.parse("2026-05-26T10:00:00Z"))
        .build();
  }
}
