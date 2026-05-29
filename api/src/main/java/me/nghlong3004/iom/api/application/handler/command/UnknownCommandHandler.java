package me.nghlong3004.iom.api.application.handler.command;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Catches any unrecognized slash command and sends guidance.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Component
@Order(98)
@RequiredArgsConstructor
public class UnknownCommandHandler implements BotMessageHandler {

  private final MessageSender messageSender;
  private final BotMessages botMessages;

  @Override
  public boolean supports(IncomingMessage message) {
    return message.isCommand();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    messageSender.send(OutgoingMessage.replyTo(message, botMessages.unknownCommandMessage()));
    return true;
  }
}
