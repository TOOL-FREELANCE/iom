package me.nghlong3004.iom.api.application.usecase;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.command.BotCommandRouter;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Service
@RequiredArgsConstructor
public class HandleIncomingMessageUseCase {

  private final BotCommandRouter commandRouter;

  public void handle(IncomingMessage message) {
    commandRouter.route(message);
  }
}
