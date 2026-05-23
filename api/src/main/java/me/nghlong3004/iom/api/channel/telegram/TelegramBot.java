package me.nghlong3004.iom.api.channel.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.config.TelegramBotProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "iom.telegram",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

  private final TelegramBotProperties properties;
  private final TelegramUpdateDispatcher dispatcher;

  @Override
  public String getBotToken() {
    return properties.botToken();
  }

  @Override
  public LongPollingUpdateConsumer getUpdatesConsumer() {
    return this;
  }

  @Override
  public void consume(Update update) {
    dispatcher.dispatch(update);
  }

  @AfterBotRegistration
  public void afterRegistration(BotSession botSession) {
    log.info("Telegram bot registered. running={}", botSession.isRunning());
  }
}
