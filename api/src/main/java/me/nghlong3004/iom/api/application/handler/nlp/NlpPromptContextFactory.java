package me.nghlong3004.iom.api.application.handler.nlp;

import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import org.springframework.stereotype.Component;

/**
 * Builds a small trusted context block for the LLM without sending long chat history or transaction
 * snapshots.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 6/1/2026
 */
@Component
public class NlpPromptContextFactory {

  public String build(ConversationContext context, String userText) {
    var recordedCount = context.getLastRecordedTransactionIds().size();
    var viewedCount = context.getLastViewedTransactionIds().size();
    var pendingAction =
        context.getPendingAction() == null ? "none" : context.getPendingAction().actionType().name();

    return """
        Trusted server context:
        - Last recorded transaction count: %d
        - Last viewed indexed transaction count: %d
        - Pending action: %s

        User message:
        %s
        """
        .formatted(recordedCount, viewedCount, pendingAction, userText);
  }
}
