package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext.PendingActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NlpPromptContextFactory Unit Tests")
class NlpPromptContextFactoryTest {

  private final NlpPromptContextFactory factory = new NlpPromptContextFactory();

  @Test
  @DisplayName("Should include bounded context counts without exposing transaction IDs")
  void build_WithContext_IncludesCountsOnly() {
    var context = new ConversationContext("TELEGRAM:user1");
    context.setLastRecordedTransactionIds(List.of(10L, 20L));
    context.setLastViewedTransactionIds(List.of(30L, 40L, 50L));

    var result = factory.build(context, "2 giao dịch đó là gì");

    assertThat(result).contains("Last recorded transaction count: 2");
    assertThat(result).contains("Last viewed indexed transaction count: 3");
    assertThat(result).contains("Pending action: none");
    assertThat(result).contains("2 giao dịch đó là gì");
    assertThat(result).doesNotContain("10", "20", "30", "40", "50");
  }

  @Test
  @DisplayName("Should include pending action type")
  void build_WithPendingAction_IncludesPendingType() {
    var context = new ConversationContext("TELEGRAM:user1");
    context.setPending(PendingActionType.DELETE, 42L, "ăn sáng");

    var result = factory.build(context, "ok");

    assertThat(result).contains("Pending action: DELETE");
  }
}
