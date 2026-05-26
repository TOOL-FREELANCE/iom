package me.nghlong3004.iom.api.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.MessageInterpreter;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.ConfirmationFormatter;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordTransactionHandler Unit Tests")
class RecordTransactionHandlerTest {

  @Mock private MessageInterpreter messageInterpreter;
  @Mock private UserResolver userResolver;
  @Mock private TransactionService transactionService;
  @Mock private MessageSender messageSender;
  @Mock private ConfirmationFormatter confirmationFormatter;

  @InjectMocks private RecordTransactionHandler handler;

  @Test
  @DisplayName("Should record transaction and send confirmation when parser returns data")
  void handle_ParsedTransaction_RecordsAndSendsConfirmation() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", " an sang 30k ");
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE,
            30000L,
            Currency.VND,
            Category.FOOD,
            "an sang",
            LocalDate.of(2026, 5, 26));
    var user = AppUser.builder().id(1L).build();
    var saved = Transaction.builder().id(10L).build();

    given(messageInterpreter.interpret("an sang 30k")).willReturn(Optional.of(parsed));
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.record(user, parsed, MessageChannel.TELEGRAM, "an sang 30k"))
        .willReturn(saved);
    given(confirmationFormatter.format(parsed)).willReturn("Da ghi nhan");

    handler.handle(message);

    var outgoingCaptor = ArgumentCaptor.forClass(OutgoingMessage.class);
    verify(messageSender).send(outgoingCaptor.capture());
    assertThat(outgoingCaptor.getValue())
        .extracting(OutgoingMessage::channel, OutgoingMessage::conversationId, OutgoingMessage::text)
        .containsExactly(MessageChannel.TELEGRAM, "chat-1", "Da ghi nhan");
  }

  @Test
  @DisplayName("Should exit without side effects when parser returns empty")
  void handle_EmptyParseResult_DoesNotRecordOrSend() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    given(messageInterpreter.interpret("hello")).willReturn(Optional.empty());

    handler.handle(message);

    verify(userResolver, never()).resolve(any());
    verify(transactionService, never()).record(any(), any(), any(), any());
    verify(messageSender, never()).send(any());
  }
}
