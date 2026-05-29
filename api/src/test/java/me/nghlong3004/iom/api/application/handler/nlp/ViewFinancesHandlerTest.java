package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.DateRangeResolverChain;
import me.nghlong3004.iom.api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ViewFinancesHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class ViewFinancesHandlerTest {

  @Mock private DateRangeResolverChain dateRangeChain;
  @Mock private FinanceViewRenderer renderer;
  @Mock private TransactionService transactionService;
  @Mock private UserResolver userResolver;
  @Mock private MessageSender messageSender;
  @Mock private ConversationContextStore contextStore;

  private ViewFinancesHandler handler;

  private final IncomingMessage textMessage =
      new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hom qua tieu bao nhieu");

  @BeforeEach
  void setUp() {
    var properties =
        new BotIntentProperties(
            new BotIntentProperties.Summary(
                List.of("xem", "tong", "bao nhieu"),
                List.of("hom nay"),
                List.of("hom qua"),
                List.of("hom kia"),
                List.of("tuan nay"),
                List.of("thang nay"),
                List.of("chi", "tieu"),
                List.of("thu", "luong"),
                List.of("mua gi", "chi gi", "lich su")),
            null);
    handler =
        new ViewFinancesHandler(
            dateRangeChain, renderer, transactionService, userResolver, messageSender, properties,
            contextStore);
  }

  @Test
  @DisplayName("Should support non-command text messages")
  void supports_NonCommandText_ReturnsTrue() {
    assertThat(handler.supports(textMessage)).isTrue();
  }

  @Test
  @DisplayName("Should not support command messages")
  void supports_Command_ReturnsFalse() {
    var cmd = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "/today");
    assertThat(handler.supports(cmd)).isFalse();
  }

  @Test
  @DisplayName("Should return false when date range cannot be resolved")
  void handle_NoDateRange_ReturnsFalse() {
    given(dateRangeChain.resolve(any())).willReturn(Optional.empty());

    assertThat(handler.handle(textMessage)).isFalse();
  }

  @Test
  @DisplayName("Should handle valid summary request")
  void handle_ValidRequest_SendsReply() {
    var range = DateRange.custom(
        Instant.parse("2026-05-26T00:00:00Z"),
        Instant.parse("2026-05-27T00:00:00Z"),
        "Hôm qua");
    var user = AppUser.builder().id(1L).build();
    given(dateRangeChain.resolve(any())).willReturn(Optional.of(range));
    given(userResolver.resolve(textMessage)).willReturn(user);
    given(transactionService.findByRange(eq(user), eq(range))).willReturn(List.of());
    given(renderer.render(eq(range), eq(ViewMode.SUMMARY), anyList(), any(), any()))
        .willReturn("Hôm qua: Chưa có giao dịch.");

    var result = handler.handle(textMessage);

    assertThat(result).isTrue();
    then(messageSender).should().send(any(OutgoingMessage.class));
  }

  @Test
  @DisplayName("Should detect detail mode from keywords")
  void handle_DetailKeyword_UsesDetailMode() {
    var detailMessage =
        new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hom qua mua gi");
    var range = DateRange.custom(
        Instant.parse("2026-05-26T00:00:00Z"),
        Instant.parse("2026-05-27T00:00:00Z"),
        "Hôm qua");
    var user = AppUser.builder().id(1L).build();
    given(dateRangeChain.resolve(any())).willReturn(Optional.of(range));
    given(userResolver.resolve(detailMessage)).willReturn(user);
    given(transactionService.findByRange(eq(user), eq(range))).willReturn(List.of());
    given(renderer.render(any(), any(ViewMode.class), anyList(), any(), any()))
        .willReturn("empty");

    handler.handle(detailMessage);

    then(messageSender).should().send(any(OutgoingMessage.class));
  }
}
