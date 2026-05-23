package me.nghlong3004.iom.api.application.command;

import lombok.RequiredArgsConstructor;
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
@Order(98)
@RequiredArgsConstructor
public class UnknownCommandHandler implements BotCommandHandler {

  private final MessageSender messageSender;

  @Override
  public boolean supports(IncomingMessage message) {
    return message.isCommand();
  }

  @Override
  public void handle(IncomingMessage message) {
    messageSender.send(
        OutgoingMessage.replyTo(
            message,
            """
                Mình chưa hỗ trợ lệnh này.

                Gõ /help để xem các lệnh hiện có.
                """));
  }
}
