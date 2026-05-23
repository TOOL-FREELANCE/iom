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
@Order(2)
@RequiredArgsConstructor
public class HelpCommandHandler implements BotCommandHandler {

  private final MessageSender messageSender;

  @Override
  public boolean supports(IncomingMessage message) {
    return BotCommand.HELP.getCommand().equalsIgnoreCase(message.normalizedText());
  }

  @Override
  public void handle(IncomingMessage message) {
    messageSender.send(
        OutgoingMessage.replyTo(
            message,
            """
                IOM hiện hỗ trợ:

                /start - Bắt đầu
                /help - Xem hướng dẫn

                Ví dụ ghi chi tiêu:
                - ăn sáng 30k
                - mua sách 120k
                - đổ xăng 50k

                Ví dụ ghi thu nhập:
                - nhận lương 5 triệu
                - được thưởng 500k
                """));
  }
}
