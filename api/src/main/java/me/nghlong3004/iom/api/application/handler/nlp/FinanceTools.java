package me.nghlong3004.iom.api.application.handler.nlp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.service.TransactionService;
import me.nghlong3004.iom.api.service.TransactionSummary;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI Tool methods for financial operations. Instantiated per request with the resolved user
 * context, so each tool method has access to the current user and conversation state.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@Slf4j
@RequiredArgsConstructor
public class FinanceTools {

  private static final int MAX_BATCH_SIZE = 10;
  private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final TransactionService transactionService;
  private final FinanceViewRenderer financeViewRenderer;
  private final ConfirmationFormatter confirmationFormatter;
  private final BotMessages botMessages;
  private final ConversationContextStore contextStore;
  private final TransactionDraftMapper transactionDraftMapper;
  private final AppUser user;
  private final String conversationKey;
  private final MessageChannel channel;
  private final String rawInput;

  @Tool(
      returnDirect = true,
      description =
          """
      Record exactly one financial transaction (income or expense).
      Prefer recordTransactions when the user message contains multiple transactions.
      Amount must be in smallest currency unit: VND as-is, USD/EUR/GBP in cents.
      """)
  String recordTransaction(
      @ToolParam(description = "INCOME or EXPENSE") String type,
      @ToolParam(description = "Amount in smallest currency unit, e.g. 30000 for '30k VND'")
          long amount,
      @ToolParam(description = "ISO currency code: VND, USD, EUR, JPY, KRW, GBP") String currency,
      @ToolParam(
              description =
                  "Category: FOOD, TRANSPORT, SALARY, EDUCATION, SHOPPING, ENTERTAINMENT, HEALTH, HOUSING, OTHER")
          String category,
      @ToolParam(description = "Short descriptive note extracted from user message") String note,
      @ToolParam(description = "Date in yyyy-MM-dd format, null for today", required = false)
          String occurredAt) {
    return recordTransactions(
        List.of(new TransactionDraft(type, amount, currency, category, note, occurredAt)));
  }

  @Tool(
      returnDirect = true,
      description =
          """
      Record one or more financial transactions from a single user message.
      Use this when the user mentions spending, earning, receiving money, or multiple separate
      finance items in one message.
      Each list item must be one transaction. Maximum 10 transactions per call.
      Amount must be in smallest currency unit: VND as-is, USD/EUR/GBP in cents.
      """)
  String recordTransactions(
      @ToolParam(description = "A list of transaction drafts parsed from the user message")
          List<TransactionDraft> transactions) {
    if (transactions == null || transactions.isEmpty()) {
      return botMessages.fallbackMessage();
    }

    if (transactions.size() > MAX_BATCH_SIZE) {
      return botMessages.transactionBatchTooLarge(MAX_BATCH_SIZE);
    }

    try {
      var parsedTransactions = transactions.stream().map(transactionDraftMapper::toParsed).toList();
      var savedTransactions =
          transactionService.recordAll(user, parsedTransactions, channel, rawInput);

      var context = contextStore.get(conversationKey);
      context.setLastRecordedTransactionIds(savedTransactions.stream().map(Transaction::getId).toList());
      contextStore.save(context);

      log.info(
          "Tool recordTransactions: count={}, userId={}",
          savedTransactions.size(),
          user.getId());
      return confirmationFormatter.formatBatch(parsedTransactions);
    } catch (RuntimeException exception) {
      log.warn("Tool recordTransactions received invalid arguments: {}", exception.toString());
      return botMessages.fallbackMessage();
    }
  }

  @Tool(
      returnDirect = true,
      description =
          """
      View financial summary or transaction history for a date range.
      Use this when user asks about spending, balance, history, or wants to see transactions.
      Vietnamese examples: "hôm nay chi bao nhiêu", "tuần này", "tháng 5 thu chi"
      English examples: "how much did I spend today", "show this month's transactions"
      """)
  String viewFinances(
      @ToolParam(description = "Start date in yyyy-MM-dd format") String from,
      @ToolParam(description = "End date in yyyy-MM-dd format (exclusive, day after last day)")
          String to,
      @ToolParam(description = "Human-readable label for the period, e.g. 'Hôm nay', 'Tháng 5/2026'")
          String label,
      @ToolParam(description = "Flow filter: ALL, INCOME, or EXPENSE") String filter,
      @ToolParam(description = "View mode: SUMMARY (totals only) or DETAIL (individual transactions)")
          String viewMode) {

    DateRange dateRange;
    FlowFilter flowFilter;
    ViewMode mode;
    try {
      var fromInstant = LocalDate.parse(from).atStartOfDay(VIETNAM_ZONE).toInstant();
      var toInstant = LocalDate.parse(to).atStartOfDay(VIETNAM_ZONE).toInstant();
      dateRange = DateRange.custom(fromInstant, toInstant, label);
      flowFilter = parseEnum(FlowFilter.class, filter);
      mode = parseEnum(ViewMode.class, viewMode);
    } catch (RuntimeException exception) {
      log.warn("Tool viewFinances received invalid arguments: {}", exception.toString());
      return botMessages.fallbackMessage();
    }

    var transactions = transactionService.findByRange(user, dateRange);
    var summary = TransactionSummary.from(transactions);
    var effectiveMode = autoAdjustViewMode(mode, transactions.size());

    saveViewedIdsIfIndexed(effectiveMode, transactions, flowFilter);

    log.info(
        "Tool viewFinances: range={}, filter={}, mode={}, count={}",
        label,
        filter,
        effectiveMode,
        transactions.size());
    return financeViewRenderer.render(dateRange, effectiveMode, transactions, summary, flowFilter);
  }

  @Tool(
      returnDirect = true,
      description =
          """
      Delete a transaction. Use when user says "xóa", "xoá", "bỏ", "delete" referring to a transaction.
      referenceType determines how to find the transaction:
        - LATEST: delete the most recently recorded transaction
        - BY_INDEX: delete by index number from the last viewed list (1-based)
      """)
  String deleteTransaction(
      @ToolParam(description = "How to reference the transaction: LATEST or BY_INDEX")
          String referenceType,
      @ToolParam(description = "Index number (1-based) when referenceType is BY_INDEX", required = false)
          Integer index) {

    var context = contextStore.get(conversationKey);
    Long transactionId = resolveTransactionId(referenceType, index, context);

    if (transactionId == null) {
      return missingReferenceMessage(referenceType);
    }

    var transaction = transactionService.findByUserAndId(user, transactionId);
    if (transaction.isEmpty()) {
      return botMessages.manageNotFound();
    }

    var tx = transaction.get();
    var desc = transactionDescription(tx);
    context.setPending(PendingActionType.DELETE, transactionId, desc);
    contextStore.save(context);

    log.info("Tool deleteTransaction: pending confirm for txId={}", transactionId);
    return botMessages.manageConfirmDelete(desc);
  }

  @Tool(
      returnDirect = true,
      description =
          """
      Start confirmation to undo the last recorded transaction action. If the last action recorded
      multiple transactions, all transactions from that action will be deleted only after the user
      confirms.
      Use when user says "undo", "hoàn tác", "bỏ cái vừa rồi".
      """)
  String undoLastTransaction() {
    var context = contextStore.get(conversationKey);

    if (!context.hasLastRecorded()) {
      return botMessages.manageNoRecent();
    }

    var transactionIds = List.copyOf(context.getLastRecordedTransactionIds());
    var foundTransactions =
        transactionIds.stream()
            .map(transactionId -> transactionService.findByUserAndId(user, transactionId))
            .flatMap(java.util.Optional::stream)
            .toList();

    if (foundTransactions.isEmpty()) {
      context.setLastRecordedTransactionIds(List.of());
      contextStore.save(context);
      return botMessages.manageNotFound();
    }

    var desc =
        foundTransactions.size() == 1
            ? transactionDescription(foundTransactions.getFirst())
            : foundTransactions.size() + " giao dịch gần nhất";
    context.setPending(
        PendingActionType.DELETE, foundTransactions.stream().map(Transaction::getId).toList(), desc);
    contextStore.save(context);

    log.info("Tool undoLastTransaction: pending confirm for count={}", foundTransactions.size());
    return botMessages.manageConfirmDelete(desc);
  }

  private void saveViewedIdsIfIndexed(
      ViewMode effectiveMode, List<Transaction> transactions, FlowFilter flowFilter) {
    if (effectiveMode == ViewMode.SUMMARY) {
      return;
    }

    var context = contextStore.get(conversationKey);
    context.setLastViewedTransactionIds(
        filterTransactions(transactions, flowFilter).stream().map(Transaction::getId).toList());
    contextStore.save(context);
  }

  private List<Transaction> filterTransactions(List<Transaction> transactions, FlowFilter filter) {
    if (filter == null || filter == FlowFilter.ALL) {
      return transactions;
    }
    var targetType =
        filter == FlowFilter.EXPENSE ? TransactionType.EXPENSE : TransactionType.INCOME;
    return transactions.stream().filter(tx -> tx.getType() == targetType).toList();
  }

  private ViewMode autoAdjustViewMode(ViewMode requestedMode, int transactionCount) {
    if (requestedMode != ViewMode.DETAIL || transactionCount == 0) {
      return requestedMode;
    }
    return transactionCount <= 10 ? ViewMode.COMPACT : ViewMode.SUMMARY;
  }

  private String missingReferenceMessage(String referenceType) {
    var normalizedReferenceType = referenceType == null ? "" : referenceType.trim().toUpperCase();
    return switch (normalizedReferenceType) {
      case "LATEST" -> botMessages.manageNoRecent();
      case "BY_INDEX" -> botMessages.manageNoList();
      default -> botMessages.manageNotFound();
    };
  }

  private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(enumType.getSimpleName() + " is required");
    }
    return Enum.valueOf(enumType, value.trim().toUpperCase());
  }

  private Long resolveTransactionId(
      String referenceType, Integer index, ConversationContext context) {
    var normalizedReferenceType = referenceType == null ? "" : referenceType.trim().toUpperCase();
    return switch (normalizedReferenceType) {
      case "LATEST" -> context.resolveLatestRecorded();
      case "BY_INDEX" -> (index != null && context.hasViewedList())
          ? context.resolveByIndex(index)
          : null;
      default -> null;
    };
  }

  private String transactionDescription(Transaction transaction) {
    return transaction.getNote() != null && !transaction.getNote().isBlank()
        ? transaction.getNote()
        : transaction.getCategory().name();
  }
}
