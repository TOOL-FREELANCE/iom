package me.nghlong3004.iom.api.application.handler.nlp;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.common.ConfirmationFormatter;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import org.springframework.stereotype.Component;

/**
 * Creates request-scoped Spring AI tool objects with the current user and conversation context.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@Component
@RequiredArgsConstructor
public class FinanceToolsFactory {

  private final TransactionService transactionService;
  private final FinanceViewRenderer financeViewRenderer;
  private final ConfirmationFormatter confirmationFormatter;
  private final BotMessages botMessages;
  private final ConversationContextStore contextStore;
  private final TransactionDraftMapper transactionDraftMapper;

  public FinanceTools create(AppUser user, IncomingMessage message, String conversationKey) {
    return new FinanceTools(
        transactionService,
        financeViewRenderer,
        confirmationFormatter,
        botMessages,
        contextStore,
        transactionDraftMapper,
        user,
        conversationKey,
        message.channel(),
        message.normalizedText());
  }
}
