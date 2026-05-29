package me.nghlong3004.iom.api.application.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the unified {@link SummaryCommandHandler} that handles all summary slash commands
 * ({@code /today}, {@code /month}).
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@DisplayName("SummaryCommandHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class SummaryCommandHandlerTest {

  @Mock private UserResolver userResolver;
  @Mock private TransactionService transactionService;
  @Mock private MessageSender messageSender;
  @Mock private FinanceViewRenderer renderer;

  @InjectMocks private SummaryCommandHandler handler;

  private final IncomingMessage todayCommand =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today");
  private final IncomingMessage monthCommand =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/month");
  private final IncomingMessage helpCommand =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");
  private final IncomingMessage textMessage =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hom nay");

  @Test
  @DisplayName("Should support /today command")
  void supports_TodayCommand_ReturnsTrue() {
    assertThat(handler.supports(todayCommand)).isTrue();
  }

  @Test
  @DisplayName("Should support /month command")
  void supports_MonthCommand_ReturnsTrue() {
    assertThat(handler.supports(monthCommand)).isTrue();
  }

  @Test
  @DisplayName("Should not support non-summary commands")
  void supports_HelpCommand_ReturnsFalse() {
    assertThat(handler.supports(helpCommand)).isFalse();
  }

  @Test
  @DisplayName("Should not support non-command text")
  void supports_TextMessage_ReturnsFalse() {
    assertThat(handler.supports(textMessage)).isFalse();
  }

  @Test
  @DisplayName("Should handle /today and send summary reply")
  void handle_TodayCommand_SendsReply() {
    var user = AppUser.builder().id(1L).build();
    given(userResolver.resolve(todayCommand)).willReturn(user);
    given(transactionService.findByRange(eq(user), any())).willReturn(List.of());
    given(renderer.render(any(), eq(ViewMode.SUMMARY), anyList(), any(), eq(FlowFilter.ALL)))
        .willReturn("Hôm nay: Chưa có giao dịch.");

    var result = handler.handle(todayCommand);

    assertThat(result).isTrue();
    then(messageSender).should().send(any(OutgoingMessage.class));
  }

  @Test
  @DisplayName("Should handle /month and send summary reply")
  void handle_MonthCommand_SendsReply() {
    var user = AppUser.builder().id(1L).build();
    given(userResolver.resolve(monthCommand)).willReturn(user);
    given(transactionService.findByRange(eq(user), any())).willReturn(List.of());
    given(renderer.render(any(), eq(ViewMode.SUMMARY), anyList(), any(), eq(FlowFilter.ALL)))
        .willReturn("Tháng 5/2026: Chưa có giao dịch.");

    var result = handler.handle(monthCommand);

    assertThat(result).isTrue();
    then(messageSender).should().send(any(OutgoingMessage.class));
  }
}
