package me.nghlong3004.iom.api.application.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.springframework.stereotype.Component;

/**
 * Routes incoming messages to the first matching {@link BotMessageHandler} in priority order.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Component
@RequiredArgsConstructor
public class BotMessageRouter {

  private final List<BotMessageHandler> handlers;

  public void route(IncomingMessage message) {
    for (BotMessageHandler handler : handlers) {
      if (handler.supports(message) && handler.handle(message)) {
        return;
      }
    }

    throw new IllegalStateException("No message handler handled the message");
  }
}
