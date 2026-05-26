package me.nghlong3004.iom.api.application.command;

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
}
