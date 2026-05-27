package me.nghlong3004.iom.api.application.command;

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

@DisplayName("TodaySummaryHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class TodaySummaryHandlerTest {

  @Mock private UserResolver userResolver;
  @Mock private TransactionService transactionService;
  @Mock private MessageSender messageSender;
  @Mock private FinanceViewRenderer renderer;

  @InjectMocks private TodaySummaryHandler handler;

  private final IncomingMessage todayCommand =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today");
  private final IncomingMessage otherCommand =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/help");

  @Test
  @DisplayName("Should support /today command")
  void supports_TodayCommand_ReturnsTrue() {
    assertThat(handler.supports(todayCommand)).isTrue();
  }

  @Test
  @DisplayName("Should not support other commands")
  void supports_OtherCommand_ReturnsFalse() {
    assertThat(handler.supports(otherCommand)).isFalse();
  }

  @Test
  @DisplayName("Should handle /today and send reply")
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
  @DisplayName("Should not support non-command text")
  void supports_TextMessage_ReturnsFalse() {
    var text = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hom nay");
    assertThat(handler.supports(text)).isFalse();
  }
}
