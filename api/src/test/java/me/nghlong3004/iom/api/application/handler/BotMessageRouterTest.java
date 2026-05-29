package me.nghlong3004.iom.api.application.handler;

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
@DisplayName("BotMessageRouter Unit Tests")
class BotMessageRouterTest {

  @Mock private BotMessageHandler firstHandler;
  @Mock private BotMessageHandler secondHandler;

  @Test
  @DisplayName("Should continue to next handler when matched handler returns false")
  void route_FirstHandlerReturnsFalse_ContinuesToNextHandler() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    var router = new BotMessageRouter(List.of(firstHandler, secondHandler));
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
    var router = new BotMessageRouter(List.of(firstHandler, secondHandler));
    given(firstHandler.supports(message)).willReturn(true);
    given(firstHandler.handle(message)).willReturn(true);

    router.route(message);

    verify(secondHandler, never()).supports(message);
    verify(secondHandler, never()).handle(message);
  }

  @Test
  @DisplayName("Should skip handler when supports returns false")
  void route_HandlerNotSupports_SkipsToNextHandler() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    var router = new BotMessageRouter(List.of(firstHandler, secondHandler));
    given(firstHandler.supports(message)).willReturn(false);
    given(secondHandler.supports(message)).willReturn(true);
    given(secondHandler.handle(message)).willReturn(true);

    router.route(message);

    verify(firstHandler, never()).handle(message);
    verify(secondHandler).handle(message);
  }

  @Test
  @DisplayName("Should throw IllegalStateException when no handler handles the message")
  void route_NoHandlerHandles_ThrowsIllegalStateException() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    var router = new BotMessageRouter(List.of(firstHandler));
    given(firstHandler.supports(message)).willReturn(true);
    given(firstHandler.handle(message)).willReturn(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> router.route(message))
        .isInstanceOf(IllegalStateException.class);
  }
}
