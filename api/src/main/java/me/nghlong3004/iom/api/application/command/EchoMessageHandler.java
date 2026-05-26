package me.nghlong3004.iom.api.application.command;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Component
@Order(99)
@RequiredArgsConstructor
public class EchoMessageHandler implements BotCommandHandler {

  private final MessageSender messageSender;
  private final BotMessages botMessages;

  @Override
  public boolean supports(IncomingMessage message) {
    return message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    messageSender.send(OutgoingMessage.replyTo(message, botMessages.fallbackMessage()));
    return true;
  }
}
