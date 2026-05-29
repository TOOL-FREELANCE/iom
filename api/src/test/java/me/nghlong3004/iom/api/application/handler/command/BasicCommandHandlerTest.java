package me.nghlong3004.iom.api.application.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.application.handler.nlp.EchoMessageHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Basic Command Handler Unit Tests")
class BasicCommandHandlerTest {

  @Mock private MessageSender messageSender;
  @Mock private BotMessages botMessages;

  @Test
  @DisplayName("Start command should send configured start message")
  void startHandle_StartCommand_SendsStartMessage() {
    var handler = new StartCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/start");
    given(botMessages.startMessage()).willReturn("start");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("start");
  }

  @Test
  @DisplayName("Help command should send configured help message")
  void helpHandle_HelpCommand_SendsHelpMessage() {
    var handler = new HelpCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");
    given(botMessages.helpMessage()).willReturn("help");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("help");
  }

  @Test
  @DisplayName("Unknown command should send configured unknown command message")
  void unknownHandle_UnknownCommand_SendsUnknownMessage() {
    var handler = new UnknownCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/unknown");
    given(botMessages.unknownCommandMessage()).willReturn("unknown");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("unknown");
  }

  @Test
  @DisplayName("Fallback command should send configured fallback message")
  void echoHandle_TextMessage_SendsFallbackMessage() {
    var handler = new EchoMessageHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    given(botMessages.fallbackMessage()).willReturn("fallback");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("fallback");
  }

  @Test
  @DisplayName("Start handler should support /start command")
  void startSupports_StartCommand_ReturnsTrue() {
    var handler = new StartCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/start");

    assertThat(handler.supports(message)).isTrue();
  }

  @Test
  @DisplayName("Start handler should not support other commands")
  void startSupports_OtherCommand_ReturnsFalse() {
    var handler = new StartCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");

    assertThat(handler.supports(message)).isFalse();
  }

  @Test
  @DisplayName("Help handler should support /help command")
  void helpSupports_HelpCommand_ReturnsTrue() {
    var handler = new HelpCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");

    assertThat(handler.supports(message)).isTrue();
  }

  @Test
  @DisplayName("Unknown command handler should support any command text")
  void unknownSupports_CommandText_ReturnsTrue() {
    var handler = new UnknownCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/foo");

    assertThat(handler.supports(message)).isTrue();
  }

  @Test
  @DisplayName("Unknown command handler should not support non-command text")
  void unknownSupports_NonCommandText_ReturnsFalse() {
    var handler = new UnknownCommandHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");

    assertThat(handler.supports(message)).isFalse();
  }

  @Test
  @DisplayName("Echo handler should support any text with content")
  void echoSupports_TextWithContent_ReturnsTrue() {
    var handler = new EchoMessageHandler(messageSender, botMessages);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");

    assertThat(handler.supports(message)).isTrue();
  }

  private void assertSentText(String expectedText) {
    var captor = ArgumentCaptor.forClass(OutgoingMessage.class);
    verify(messageSender).send(captor.capture());
    assertThat(captor.getValue().text()).isEqualTo(expectedText);
  }
}
