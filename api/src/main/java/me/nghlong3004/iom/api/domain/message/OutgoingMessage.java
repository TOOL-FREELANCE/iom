package me.nghlong3004.iom.api.domain.message;

import me.nghlong3004.iom.api.domain.MessageChannel;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
public record OutgoingMessage(MessageChannel channel, String conversationId, String text) {

  public static OutgoingMessage replyTo(IncomingMessage incomingMessage, String text) {
    return new OutgoingMessage(incomingMessage.channel(), incomingMessage.conversationId(), text);
  }
}
