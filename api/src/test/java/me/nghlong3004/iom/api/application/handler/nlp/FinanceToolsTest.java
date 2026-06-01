package me.nghlong3004.iom.api.application.handler.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.ConversationContextStore;
import me.nghlong3004.iom.api.common.BotMessages;
import me.nghlong3004.iom.api.common.ConfirmationFormatter;
import me.nghlong3004.iom.api.common.FinanceViewRenderer;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext;
import me.nghlong3004.iom.api.domain.conversation.ConversationContext.PendingActionType;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.summary.FlowFilter;
import me.nghlong3004.iom.api.domain.summary.ViewMode;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("FinanceTools Unit Tests")
@ExtendWith(MockitoExtension.class)
class FinanceToolsTest {

  @Mock private TransactionService transactionService;
  @Mock private FinanceViewRenderer financeViewRenderer;
  @Mock private ConfirmationFormatter confirmationFormatter;
  @Mock private BotMessages botMessages;
  @Mock private ConversationContextStore contextStore;
  @Mock private TransactionDraftMapper transactionDraftMapper;

  private AppUser user;
  private ConversationContext context;
  private FinanceTools tools;

  @BeforeEach
  void setUp() {
    user = mock(AppUser.class);
    context = new ConversationContext("TELEGRAM:user1");
    lenient().when(contextStore.get("TELEGRAM:user1")).thenReturn(context);

    tools =
        new FinanceTools(
            transactionService,
            financeViewRenderer,
            confirmationFormatter,
            botMessages,
            contextStore,
            transactionDraftMapper,
            user,
            "TELEGRAM:user1",
            MessageChannel.TELEGRAM,
            "ăn sáng 30k");
  }

  @Nested
  @DisplayName("recordTransaction()")
  class RecordTransaction {

    @Test
    @DisplayName("Should record a single transaction via batch path")
    void recordTransaction_ValidInput_RecordsAndConfirms() {
      when(user.getId()).thenReturn(1L);
      var draft = new TransactionDraft("EXPENSE", 30000, "VND", "FOOD", "ăn sáng", null);
      var parsed = parsed(TransactionType.EXPENSE, 30000L, "ăn sáng", null);
      var savedTx = transaction(42L, Currency.VND, TransactionType.EXPENSE, 30000L);
      when(transactionDraftMapper.toParsed(draft)).thenReturn(parsed);
      when(transactionService.recordAll(eq(user), eq(List.of(parsed)), any(), any()))
          .thenReturn(List.of(savedTx));
      when(confirmationFormatter.formatBatch(List.of(parsed))).thenReturn("Đã ghi nhận.");

      var result = tools.recordTransaction("EXPENSE", 30000, "VND", "FOOD", "ăn sáng", null);

      assertThat(result).isEqualTo("Đã ghi nhận.");
      verify(transactionService)
          .recordAll(eq(user), eq(List.of(parsed)), eq(MessageChannel.TELEGRAM), eq("ăn sáng 30k"));
      assertThat(context.getLastRecordedTransactionIds()).containsExactly(42L);
    }

    @Test
    @DisplayName("Should map specific date when occurredAt is provided")
    void recordTransaction_WithDate_UsesMapperResult() {
      var draft = new TransactionDraft("EXPENSE", 50000, "VND", "FOOD", "ăn trưa", "2026-05-28");
      var parsed = parsed(TransactionType.EXPENSE, 50000L, "ăn trưa", LocalDate.of(2026, 5, 28));
      var savedTx = transaction(1L, Currency.VND, TransactionType.EXPENSE, 50000L);
      when(user.getId()).thenReturn(1L);
      when(transactionDraftMapper.toParsed(draft)).thenReturn(parsed);
      when(transactionService.recordAll(eq(user), eq(List.of(parsed)), any(), any()))
          .thenReturn(List.of(savedTx));
      when(confirmationFormatter.formatBatch(List.of(parsed))).thenReturn("done");

      tools.recordTransaction("EXPENSE", 50000, "VND", "FOOD", "ăn trưa", "2026-05-28");

      verify(transactionDraftMapper).toParsed(draft);
    }
  }

  @Nested
  @DisplayName("recordTransactions()")
  class RecordTransactions {

    @Test
    @DisplayName("Should record multiple transactions all-or-nothing")
    void recordTransactions_ValidBatch_RecordsAllAndStoresIds() {
      when(user.getId()).thenReturn(1L);
      var draft1 = new TransactionDraft("EXPENSE", 80000, "VND", "FOOD", "bún bò", null);
      var draft2 = new TransactionDraft("EXPENSE", 60000, "VND", "FOOD", "trà sữa", null);
      var parsed1 = parsed(TransactionType.EXPENSE, 80000L, "bún bò", null);
      var parsed2 = parsed(TransactionType.EXPENSE, 60000L, "trà sữa", null);
      var saved1 = transaction(10L, Currency.VND, TransactionType.EXPENSE, 80000L);
      var saved2 = transaction(20L, Currency.VND, TransactionType.EXPENSE, 60000L);
      when(transactionDraftMapper.toParsed(draft1)).thenReturn(parsed1);
      when(transactionDraftMapper.toParsed(draft2)).thenReturn(parsed2);
      when(transactionService.recordAll(eq(user), eq(List.of(parsed1, parsed2)), any(), any()))
          .thenReturn(List.of(saved1, saved2));
      when(confirmationFormatter.formatBatch(List.of(parsed1, parsed2)))
          .thenReturn("Đã ghi nhận 2 giao dịch");

      var result = tools.recordTransactions(List.of(draft1, draft2));

      assertThat(result).isEqualTo("Đã ghi nhận 2 giao dịch");
      assertThat(context.getLastRecordedTransactionIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("Should not persist when any draft is invalid")
    void recordTransactions_InvalidDraft_DoesNotPersist() {
      var draft = new TransactionDraft("BAD", 80000, "VND", "FOOD", "bún bò", null);
      when(transactionDraftMapper.toParsed(draft)).thenThrow(new IllegalArgumentException("bad"));
      when(botMessages.fallbackMessage()).thenReturn("fallback");

      var result = tools.recordTransactions(List.of(draft));

      assertThat(result).isEqualTo("fallback");
      verify(transactionService, never()).recordAll(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject batches larger than limit")
    void recordTransactions_TooLarge_DoesNotPersist() {
      var drafts =
          java.util.stream.IntStream.range(0, 11)
              .mapToObj(i -> new TransactionDraft("EXPENSE", 1, "VND", "FOOD", "n" + i, null))
              .toList();
      when(botMessages.transactionBatchTooLarge(10)).thenReturn("too large");

      var result = tools.recordTransactions(drafts);

      assertThat(result).isEqualTo("too large");
      verify(transactionService, never()).recordAll(any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("viewFinances()")
  class ViewFinances {

    @Test
    @DisplayName("Should fetch transactions and render view")
    void viewFinances_ValidRange_RendersView() {
      var txList = List.of(transaction(1L, Currency.VND, TransactionType.EXPENSE, 30000L));
      when(transactionService.findByRange(eq(user), any(DateRange.class))).thenReturn(txList);
      when(financeViewRenderer.render(
              any(DateRange.class),
              eq(ViewMode.SUMMARY),
              eq(txList),
              any(TransactionSummary.class),
              eq(FlowFilter.ALL)))
          .thenReturn("Hôm nay: Chi 30.000 VND");

      var result = tools.viewFinances("2026-05-29", "2026-05-30", "Hôm nay", "ALL", "SUMMARY");

      assertThat(result).isEqualTo("Hôm nay: Chi 30.000 VND");
      assertThat(context.getLastViewedTransactionIds()).isEmpty();
    }

    @Test
    @DisplayName("Should store viewed transaction IDs only for indexed views")
    void viewFinances_Detail_StoresViewedIds() {
      var tx1 = transaction(10L, Currency.VND, TransactionType.EXPENSE, 30000L);
      var tx2 = transaction(20L, Currency.VND, TransactionType.INCOME, 100000L);
      when(transactionService.findByRange(eq(user), any())).thenReturn(List.of(tx1, tx2));
      when(financeViewRenderer.render(any(), any(), any(), any(), any())).thenReturn("result");

      tools.viewFinances("2026-05-29", "2026-05-30", "Hôm nay", "ALL", "DETAIL");

      assertThat(context.getLastViewedTransactionIds()).containsExactly(10L, 20L);
    }
  }

  @Nested
  @DisplayName("viewReferencedTransactions()")
  class ViewReferencedTransactions {

    @Test
    @DisplayName("Should render last viewed transactions when using auto reference")
    void viewReferencedTransactions_Auto_UsesLastViewedIds() {
      context.setLastRecordedTransactionIds(List.of(1L));
      context.setLastViewedTransactionIds(List.of(10L, 20L));
      var tx1 = transaction(10L, Currency.VND, TransactionType.EXPENSE, 80000L);
      var tx2 = transaction(20L, Currency.VND, TransactionType.EXPENSE, 60000L);
      var txList = List.of(tx1, tx2);
      when(transactionService.findAllByUserAndIds(user, List.of(10L, 20L))).thenReturn(txList);
      when(financeViewRenderer.render(
              any(DateRange.class),
              eq(ViewMode.DETAIL),
              eq(txList),
              any(TransactionSummary.class),
              eq(FlowFilter.ALL)))
          .thenReturn("Các giao dịch vừa nhắc tới:\n1. bún bò\n2. trà sữa");

      var result = tools.viewReferencedTransactions("AUTO");

      assertThat(result).contains("bún bò");
      assertThat(context.getLastViewedTransactionIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("Should return no-list message when there is no referenced list")
    void viewReferencedTransactions_NoContext_ReturnsNoList() {
      when(botMessages.manageNoList()).thenReturn("no list");

      var result = tools.viewReferencedTransactions("AUTO");

      assertThat(result).isEqualTo("no list");
      verify(transactionService, never()).findAllByUserAndIds(any(), any());
    }
  }

  @Nested
  @DisplayName("deleteTransaction()")
  class DeleteTransaction {

    @Test
    @DisplayName("Should use latest transaction from recorded batch")
    void deleteTransaction_Latest_UsesLastRecordedId() {
      context.setLastRecordedTransactionIds(List.of(10L, 20L));

      var tx = transaction(20L, Currency.VND, TransactionType.EXPENSE, 60000L);
      when(tx.getNote()).thenReturn("trà sữa");
      when(transactionService.findByUserAndId(user, 20L)).thenReturn(Optional.of(tx));
      when(botMessages.manageConfirmDelete("trà sữa")).thenReturn("confirm trà sữa ok");

      var result = tools.deleteTransaction("LATEST", null);

      assertThat(result).contains("trà sữa");
      assertThat(context.isAwaitingConfirmation()).isTrue();
      assertThat(context.getPendingAction().actionType()).isEqualTo(PendingActionType.DELETE);
      assertThat(context.getPendingAction().transactionId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should return not-found when no recent transaction")
    void deleteTransaction_NoRecent_ReturnsNotFound() {
      when(transactionService.findLatestByUser(user)).thenReturn(Optional.empty());
      when(botMessages.manageNoRecent()).thenReturn("no recent");

      var result = tools.deleteTransaction("LATEST", null);

      assertThat(result).isEqualTo("no recent");
      assertThat(context.isAwaitingConfirmation()).isFalse();
    }

    @Test
    @DisplayName("Should fall back to latest persisted transaction when context is empty")
    void deleteTransaction_LatestWithoutContext_UsesLatestPersistedTransaction() {
      var tx = transaction(99L, Currency.VND, TransactionType.EXPENSE, 120000L);
      when(tx.getNote()).thenReturn("cà phê");
      when(transactionService.findLatestByUser(user)).thenReturn(Optional.of(tx));
      when(transactionService.findByUserAndId(user, 99L)).thenReturn(Optional.of(tx));
      when(botMessages.manageConfirmDelete("cà phê")).thenReturn("confirm cà phê ok");

      var result = tools.deleteTransaction("LATEST", null);

      assertThat(result).contains("cà phê");
      assertThat(context.isAwaitingConfirmation()).isTrue();
      assertThat(context.getPendingAction().transactionId()).isEqualTo(99L);
    }
  }

  @Nested
  @DisplayName("undoLastTransaction()")
  class UndoLastTransaction {

    @Test
    @DisplayName("Should ask for confirmation before undoing last recorded batch")
    void undoLastTransaction_HasBatch_PendsDeleteConfirmation() {
      context.setLastRecordedTransactionIds(List.of(10L, 20L));

      var tx1 = transaction(10L, Currency.VND, TransactionType.EXPENSE, 80000L);
      var tx2 = transaction(20L, Currency.VND, TransactionType.EXPENSE, 60000L);
      when(transactionService.findByUserAndId(user, 10L)).thenReturn(Optional.of(tx1));
      when(transactionService.findByUserAndId(user, 20L)).thenReturn(Optional.of(tx2));
      when(botMessages.manageConfirmDelete("2 giao dịch gần nhất")).thenReturn("confirm undo 2");

      var result = tools.undoLastTransaction();

      assertThat(result).isEqualTo("confirm undo 2");
      verify(transactionService, never()).delete(any(), any());
      assertThat(context.isAwaitingConfirmation()).isTrue();
      assertThat(context.getPendingAction().actionType()).isEqualTo(PendingActionType.DELETE);
      assertThat(context.getPendingAction().transactionIds()).containsExactly(10L, 20L);
      assertThat(context.getLastRecordedTransactionIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("Should return message when nothing to undo")
    void undoLastTransaction_NoLast_ReturnsMessage() {
      when(botMessages.manageNoRecent()).thenReturn("no recent");

      var result = tools.undoLastTransaction();

      assertThat(result).isEqualTo("no recent");
      verify(transactionService, never()).delete(any(), any());
    }
  }

  private ParsedTransaction parsed(
      TransactionType type, long amount, String note, LocalDate occurredAt) {
    return new ParsedTransaction(type, amount, Currency.VND, Category.FOOD, note, occurredAt);
  }

  private Transaction transaction(Long id, Currency currency, TransactionType type, long amount) {
    var tx = mock(Transaction.class);
    lenient().when(tx.getId()).thenReturn(id);
    lenient().when(tx.getCurrency()).thenReturn(currency);
    lenient().when(tx.getType()).thenReturn(type);
    lenient().when(tx.getAmount()).thenReturn(amount);
    lenient().when(tx.getCategory()).thenReturn(Category.FOOD);
    return tx;
  }
}
