package me.nghlong3004.iom.api.channel.telegram;

import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Component
public class TelegramMessageMapper {

  public IncomingMessage toIncomingMessage(Update update) {
    var message = update.getMessage();
    var from = message.getFrom();

    return new IncomingMessage(
        MessageChannel.TELEGRAM,
        from == null ? null : String.valueOf(from.getId()),
        String.valueOf(message.getChatId()),
        message.getText());
  }
}
