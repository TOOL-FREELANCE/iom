package me.nghlong3004.iom.api.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.SummaryIntentInterpreter;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.common.SummaryFormatter;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ParsedSummaryIntent;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryIntentHandler Unit Tests")
class SummaryIntentHandlerTest {

  @Mock private UserResolver userResolver;
  @Mock private TransactionService transactionService;
  @Mock private MessageSender messageSender;
  @Mock private BotMessages botMessages;
  @Mock private SummaryFormatter summaryFormatter;
  @Mock private SummaryIntentInterpreter summaryIntentInterpreter;

  private SummaryIntentHandler handler;

  @BeforeEach
  void setUp() {
    var properties =
        new BotIntentProperties(
            new BotIntentProperties.Summary(
                List.of("xem", "tong", "bao nhieu"),
                List.of("hom nay"),
                List.of("thang nay"),
                List.of("chi"),
                List.of("thu")));
    handler =
        new SummaryIntentHandler(
            userResolver,
            transactionService,
            messageSender,
            botMessages,
            summaryFormatter,
            properties,
            summaryIntentInterpreter);
  }

  @Test
  @DisplayName("Should handle natural today summary request")
  void handle_TodaySummaryIntent_SendsTodaySummary() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "xem tong hom nay");
    var user = AppUser.builder().id(1L).build();
    var summary = new TransactionSummary(java.util.Map.of(), 0);
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.summarize(any(), any(), any())).willReturn(summary);
    given(botMessages.todayLabel()).willReturn("Today");
    given(summaryFormatter.format("Today", summary, FlowFilter.ALL)).willReturn("today summary");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("today summary");
    verify(summaryIntentInterpreter, never()).interpret(any());
  }

  @Test
  @DisplayName("Should handle natural month summary request")
  void handle_MonthSummaryIntent_SendsMonthSummary() {
    var message =
        new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "thang nay chi bao nhieu");
    var user = AppUser.builder().id(1L).build();
    var summary = new TransactionSummary(java.util.Map.of(), 0);
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.summarize(any(), any(), any())).willReturn(summary);
    given(botMessages.monthLabel(anyInt(), anyInt())).willReturn("Month");
    given(summaryFormatter.format("Month", summary, FlowFilter.EXPENSE)).willReturn("month summary");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("month summary");
    verify(summaryIntentInterpreter, never()).interpret(any());
  }

  @Test
  @DisplayName("Should use LLM summary intent when deterministic rules do not match")
  void handle_LlmSummaryIntent_SendsFilteredSummary() {
    var message =
        new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hom qua tieu bao nhieu");
    var user = AppUser.builder().id(1L).build();
    var summary = new TransactionSummary(java.util.Map.of(), 0);
    var from = Instant.parse("2026-05-25T00:00:00Z");
    var to = Instant.parse("2026-05-26T00:00:00Z");
    given(summaryIntentInterpreter.interpret("hom qua tieu bao nhieu"))
        .willReturn(
            Optional.of(ParsedSummaryIntent.summary(from, to, "Hom qua", FlowFilter.EXPENSE)));
    given(userResolver.resolve(message)).willReturn(user);
    given(transactionService.summarize(user, from, to)).willReturn(summary);
    given(summaryFormatter.format("Hom qua", summary, FlowFilter.EXPENSE))
        .willReturn("expense summary");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("expense summary");
  }

  @Test
  @DisplayName("Should send clarification when LLM summary intent asks for more details")
  void handle_ClarificationIntent_SendsClarificationReply() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "may hom truoc thi sao");
    given(summaryIntentInterpreter.interpret("may hom truoc thi sao"))
        .willReturn(Optional.of(ParsedSummaryIntent.clarification("Ban muon xem ngay nao?")));
    given(botMessages.summaryClarification("Ban muon xem ngay nao?")).willReturn("clarify");

    var handled = handler.handle(message);

    assertThat(handled).isTrue();
    assertSentText("clarify");
    verify(userResolver, never()).resolve(any());
    verify(transactionService, never()).summarize(any(), any(), any());
  }

  @Test
  @DisplayName("Should return false for unrelated text")
  void handle_UnrelatedText_ReturnsFalse() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "u-1", "chat-1", "hello");
    given(summaryIntentInterpreter.interpret("hello")).willReturn(Optional.empty());

    var handled = handler.handle(message);

    assertThat(handled).isFalse();
    verify(userResolver, never()).resolve(any());
    verify(messageSender, never()).send(any());
  }

  private void assertSentText(String expectedText) {
    var captor = ArgumentCaptor.forClass(OutgoingMessage.class);
    verify(messageSender).send(captor.capture());
    assertThat(captor.getValue().text()).isEqualTo(expectedText);
  }
}
