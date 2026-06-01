package me.nghlong3004.iom.api.application.handler.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.handler.BotMessageHandler;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.message.OutgoingMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handles all non-command text messages using Spring AI Tool Calling. Replaces the previous chain of
 * {@code RecordTransactionHandler → ManageTransactionHandler → ViewFinancesHandler →
 * EchoMessageHandler}.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li><b>Keyword guard</b> — handles "ok"/"hủy"/"undo" when context is AWAITING_CONFIRMATION,
 *       without calling the LLM. This fixes the ordering bug where RecordTransactionHandler would
 *       intercept confirmation messages.
 *   <li><b>Tool calling</b> — sends the message to DeepSeek with {@link FinanceTools} registered as
 *       tools. The LLM decides which tool to call (record, view, delete, undo) and Spring AI
 *       auto-executes it.
 * </ol>
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class NlpMessageHandler implements BotMessageHandler {

  private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
  private final FinanceToolsFactory financeToolsFactory;
  private final ConversationContextStore contextStore;
  private final UserResolver userResolver;
  private final MessageSender messageSender;
  private final BotMessages botMessages;
  private final PendingActionHandler pendingActionHandler;
  private final NlpSystemPromptFactory systemPromptFactory;
  private final NlpPromptContextFactory promptContextFactory;

  @Override
  public boolean supports(IncomingMessage message) {
    return !message.isCommand() && message.hasText();
  }

  @Override
  public boolean handle(IncomingMessage message) {
    var conversationKey = contextKey(message);
    var context = contextStore.get(conversationKey);

    if (pendingActionHandler.handleIfPending(message, context)) {
      return true;
    }

    // 2. Tool calling via Spring AI
    try {
      var user = userResolver.resolve(message);
      var financeTools = financeToolsFactory.create(user, message, conversationKey);

      var response =
          chatClientBuilderProvider
              .getObject()
              .build()
              .prompt()
              .system(systemPromptFactory.build())
              .user(promptContextFactory.build(context, message.normalizedText()))
              .tools(financeTools)
              .call()
              .content();

      var reply = (response != null && !response.isBlank()) ? response : botMessages.fallbackMessage();
      messageSender.send(OutgoingMessage.replyTo(message, reply));
    } catch (RuntimeException exception) {
      log.warn("LLM tool calling failed: {}", exception.toString());
      messageSender.send(OutgoingMessage.replyTo(message, botMessages.fallbackMessage()));
    }

    return true;
  }

  private String contextKey(IncomingMessage message) {
    return message.channel() + ":" + message.externalUserId();
  }
}
