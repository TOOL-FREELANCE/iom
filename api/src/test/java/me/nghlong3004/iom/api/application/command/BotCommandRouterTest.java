package me.nghlong3004.iom.api.application.command;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BotCommandRouter Unit Tests")
class BotCommandRouterTest {

  @Mock private BotCommandHandler firstHandler;
  @Mock private BotCommandHandler secondHandler;

  @Test
  @DisplayName("Should continue to next handler when matched handler returns false")
  void route_FirstHandlerReturnsFalse_ContinuesToNextHandler() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    var router = new BotCommandRouter(List.of(firstHandler, secondHandler));
    given(firstHandler.supports(message)).willReturn(true);
    given(firstHandler.handle(message)).willReturn(false);
    given(secondHandler.supports(message)).willReturn(true);
    given(secondHandler.handle(message)).willReturn(true);

    router.route(message);

    verify(firstHandler).handle(message);
    verify(secondHandler).handle(message);
  }

  @Test
  @DisplayName("Should stop when first matching handler handles message")
  void route_FirstHandlerReturnsTrue_StopsRouting() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");
    var router = new BotCommandRouter(List.of(firstHandler, secondHandler));
    given(firstHandler.supports(message)).willReturn(true);
    given(firstHandler.handle(message)).willReturn(true);

    router.route(message);

    verify(secondHandler, never()).supports(message);
    verify(secondHandler, never()).handle(message);
  }
}
