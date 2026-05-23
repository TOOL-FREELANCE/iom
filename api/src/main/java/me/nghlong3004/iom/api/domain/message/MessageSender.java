package me.nghlong3004.iom.api.domain.message;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
public interface MessageSender {

  void send(OutgoingMessage message);
}
