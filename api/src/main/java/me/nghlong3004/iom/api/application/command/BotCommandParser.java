package me.nghlong3004.iom.api.application.command;

import me.nghlong3004.iom.api.domain.message.IncomingMessage;

/**
 * Normalizes Telegram bot commands, including group-chat suffixes such as {@code /today@iom_bot}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
final class BotCommandParser {

  private BotCommandParser() {}

  static boolean matches(IncomingMessage message, BotCommand command) {
    return command.getCommand().equalsIgnoreCase(normalize(message.normalizedText()));
  }

  static String normalize(String text) {
    if (text == null || !text.startsWith("/")) {
      return "";
    }

    var firstToken = text.trim().split("\\s+", 2)[0];
    var mentionIndex = firstToken.indexOf('@');
    return mentionIndex >= 0 ? firstToken.substring(0, mentionIndex) : firstToken;
  }
}
