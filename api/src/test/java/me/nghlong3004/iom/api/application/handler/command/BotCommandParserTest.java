package me.nghlong3004.iom.api.application.handler.command;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@DisplayName("BotCommandParser Unit Tests")
class BotCommandParserTest {

  @Test
  @DisplayName("Should match Telegram command with bot username suffix")
  void matches_CommandWithBotSuffix_ReturnsTrue() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today@iom_bot");

    var result = BotCommandParser.matches(message, BotCommand.TODAY);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should match command with trailing arguments")
  void matches_CommandWithArguments_ReturnsTrue() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/month now");

    var result = BotCommandParser.matches(message, BotCommand.MONTH);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should match exact command without suffix")
  void matches_ExactCommand_ReturnsTrue() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today");

    var result = BotCommandParser.matches(message, BotCommand.TODAY);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should not match different command")
  void matches_DifferentCommand_ReturnsFalse() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");

    var result = BotCommandParser.matches(message, BotCommand.TODAY);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not match non-command text")
  void matches_NonCommandText_ReturnsFalse() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");

    var result = BotCommandParser.matches(message, BotCommand.TODAY);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not match null text")
  void matches_NullText_ReturnsFalse() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", null);

    var result = BotCommandParser.matches(message, BotCommand.TODAY);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("normalize should return empty for null input")
  void normalize_NullInput_ReturnsEmpty() {
    assertThat(BotCommandParser.normalize(null)).isEmpty();
  }

  @Test
  @DisplayName("normalize should return empty for non-slash text")
  void normalize_NonSlashText_ReturnsEmpty() {
    assertThat(BotCommandParser.normalize("hello")).isEmpty();
  }
}
