package me.nghlong3004.iom.api.domain.message;

import me.nghlong3004.iom.api.domain.MessageChannel;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
public record IncomingMessage(
    MessageChannel channel, String externalUserId, String conversationId, String text) {

  public String normalizedText() {
    return text == null ? "" : text.trim();
  }

  public boolean hasText() {
    return !normalizedText().isBlank();
  }

  public boolean isCommand() {
    return normalizedText().startsWith("/");
  }
}
