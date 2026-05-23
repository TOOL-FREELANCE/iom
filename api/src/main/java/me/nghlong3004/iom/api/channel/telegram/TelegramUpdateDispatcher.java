package me.nghlong3004.iom.api.channel.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.usecase.HandleIncomingMessageUseCase;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
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
public class TelegramUpdateDispatcher {

  private final TelegramMessageMapper mapper;
  private final HandleIncomingMessageUseCase handleIncomingMessageUseCase;

  public void dispatch(Update update) {
    if (!isSupported(update)) {
      log.debug("Unsupported Telegram update: {}", update);
      return;
    }

    IncomingMessage message = mapper.toIncomingMessage(update);

    log.info(
        "Received Telegram message. externalUserId={}, conversationId={}, text={}",
        message.externalUserId(),
        message.conversationId(),
        message.normalizedText());

    handleIncomingMessageUseCase.handle(message);
  }

  private boolean isSupported(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getChatId() != null;
  }
}
