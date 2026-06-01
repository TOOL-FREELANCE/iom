package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import me.nghlong3004.iom.api.domain.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("NlpMessageHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class NlpMessageHandlerTest {

  @Mock private ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
  @Mock private ChatClient.Builder chatClientBuilder;
  @Mock private FinanceToolsFactory financeToolsFactory;
  @Mock private ConversationContextStore contextStore;
  @Mock private UserResolver userResolver;
  @Mock private MessageSender messageSender;
  @Mock private BotMessages botMessages;
  @Mock private PendingActionHandler pendingActionHandler;
  @Mock private NlpSystemPromptFactory systemPromptFactory;

  private NlpMessageHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new NlpMessageHandler(
            chatClientBuilderProvider,
            financeToolsFactory,
            contextStore,
            userResolver,
            messageSender,
            botMessages,
            pendingActionHandler,
            systemPromptFactory);
  }

  @Nested
  @DisplayName("supports()")
  class Supports {

    @Test
    @DisplayName("Should support non-command text messages")
    void supports_NonCommandText_ReturnsTrue() {
      var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "ăn sáng 30k");
      assertThat(handler.supports(message)).isTrue();
    }

    @Test
    @DisplayName("Should not support command messages")
    void supports_Command_ReturnsFalse() {
      var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "/tonghop");
      assertThat(handler.supports(message)).isFalse();
    }

    @Test
    @DisplayName("Should not support blank messages")
    void supports_Blank_ReturnsFalse() {
      var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "   ");
      assertThat(handler.supports(message)).isFalse();
    }
  }

  @Test
  @DisplayName("Should stop when pending action handler handles message")
  void handle_PendingHandled_DoesNotCallLlm() {
    var context = new ConversationContext("TELEGRAM:user1");
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "ok");
    when(contextStore.get("TELEGRAM:user1")).thenReturn(context);
    when(pendingActionHandler.handleIfPending(message, context)).thenReturn(true);

    var result = handler.handle(message);

    assertThat(result).isTrue();
    verifyNoInteractions(chatClientBuilderProvider, userResolver, financeToolsFactory);
  }

  @Test
  @DisplayName("Should use prompt factory and send LLM response")
  void handle_NonPending_DelegatesToLlm() {
    var context = new ConversationContext("TELEGRAM:user1");
    var user = mock(AppUser.class);
    var chatClient = mock(ChatClient.class);
    var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
    var callResponse = mock(ChatClient.CallResponseSpec.class);
    var financeTools = mock(FinanceTools.class);
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "user1", "conv1", "ăn trưa 50k");

    when(contextStore.get("TELEGRAM:user1")).thenReturn(context);
    when(pendingActionHandler.handleIfPending(message, context)).thenReturn(false);
    when(userResolver.resolve(message)).thenReturn(user);
    when(financeToolsFactory.create(user, message, "TELEGRAM:user1")).thenReturn(financeTools);
    when(systemPromptFactory.build()).thenReturn("system prompt");
    when(chatClientBuilderProvider.getObject()).thenReturn(chatClientBuilder);
    when(chatClientBuilder.build()).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(promptSpec);
    when(promptSpec.system("system prompt")).thenReturn(promptSpec);
    when(promptSpec.user("ăn trưa 50k")).thenReturn(promptSpec);
    when(promptSpec.tools(financeTools)).thenReturn(promptSpec);
    when(promptSpec.call()).thenReturn(callResponse);
    when(callResponse.content()).thenReturn("Đã ghi nhận.");

    var result = handler.handle(message);

    assertThat(result).isTrue();
    var captor = ArgumentCaptor.forClass(OutgoingMessage.class);
    verify(messageSender).send(captor.capture());
    assertThat(captor.getValue().text()).isEqualTo("Đã ghi nhận.");
  }
}

