package me.nghlong3004.iom.api.channel.telegram;

import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.config.TelegramBotProperties;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "iom.telegram",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TelegramReplySender implements MessageSender {

  private final TelegramClient telegramClient;

  public TelegramReplySender(TelegramBotProperties properties) {
    this.telegramClient = new OkHttpTelegramClient(properties.botToken());
  }

  @Override
  public void send(OutgoingMessage message) {
    if (message.channel() != MessageChannel.TELEGRAM) {
      log.debug("Skip non-Telegram outgoing message. channel={}", message.channel());
      return;
    }

    SendMessage sendMessage =
        SendMessage.builder().chatId(message.conversationId()).text(message.text()).build();

    try {
      telegramClient.execute(sendMessage);
    } catch (TelegramApiException exception) {
      log.error(
          "Failed to send Telegram message. conversationId={}",
          message.conversationId(),
          exception);
    }
  }
}
