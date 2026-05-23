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
@Order(1)
@RequiredArgsConstructor
public class StartCommandHandler implements BotCommandHandler {

  private final MessageSender messageSender;

  @Override
  public boolean supports(IncomingMessage message) {
    return BotCommand.START.getCommand().equalsIgnoreCase(message.normalizedText());
  }

  @Override
  public void handle(IncomingMessage message) {
    messageSender.send(
        OutgoingMessage.replyTo(
            message,
            """
                Xin chào, mình là IOM - Input Output Money.

                Bạn có thể ghi thu/chi bằng cách nhắn:
                - ăn sáng 30k
                - đổ xăng 50k
                - nhận lương 5 triệu

                Gõ /help để xem hướng dẫn.
                """));
  }
}
