package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.repository.TransactionRepository;
import me.nghlong3004.iom.api.service.mapper.TransactionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private TransactionMapper transactionMapper;

  @InjectMocks private TransactionService service;

  @Test
  @DisplayName("Should map and save transaction when recording")
  void record_ValidInput_SavesTransaction() {
    var user = AppUser.builder().id(1L).build();
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE,
            30000L,
            Currency.VND,
            Category.FOOD,
            "an sang",
            LocalDate.of(2026, 5, 26));
    var transaction = Transaction.builder().id(10L).build();
    var saved = Transaction.builder().id(11L).build();

    given(transactionMapper.toEntity(user, parsed, MessageChannel.TELEGRAM, "an sang 30k"))
        .willReturn(transaction);
    given(transactionRepository.save(transaction)).willReturn(saved);

    var result = service.record(user, parsed, MessageChannel.TELEGRAM, "an sang 30k");

    assertThat(result).isSameAs(saved);
    verify(transactionRepository).save(transaction);
  }

  @Test
  @DisplayName("Should map and save all transactions atomically when recording batch")
  void recordAll_ValidInput_SavesAllTransactions() {
    var user = AppUser.builder().id(1L).build();
    var parsed1 =
        new ParsedTransaction(
            TransactionType.EXPENSE, 30000L, Currency.VND, Category.FOOD, "an sang", null);
    var parsed2 =
        new ParsedTransaction(
            TransactionType.EXPENSE, 50000L, Currency.VND, Category.FOOD, "an trua", null);
    var tx1 = Transaction.builder().id(10L).build();
    var tx2 = Transaction.builder().id(20L).build();
    var saved1 = Transaction.builder().id(11L).build();
    var saved2 = Transaction.builder().id(21L).build();

    given(transactionMapper.toEntity(user, parsed1, MessageChannel.TELEGRAM, "batch"))
        .willReturn(tx1);
    given(transactionMapper.toEntity(user, parsed2, MessageChannel.TELEGRAM, "batch"))
        .willReturn(tx2);
    given(transactionRepository.saveAll(List.of(tx1, tx2))).willReturn(List.of(saved1, saved2));

    var result =
        service.recordAll(user, List.of(parsed1, parsed2), MessageChannel.TELEGRAM, "batch");

    assertThat(result).containsExactly(saved1, saved2);
    verify(transactionRepository).saveAll(List.of(tx1, tx2));
  }

  @Test
  @DisplayName("Should reject empty batch")
  void recordAll_EmptyBatch_ThrowsException() {
    var user = AppUser.builder().id(1L).build();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.recordAll(user, List.of(), MessageChannel.TELEGRAM, "batch"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("parsedTransactions must not be empty");
    verify(transactionRepository, org.mockito.Mockito.never()).saveAll(any());
  }

  @Test
  @DisplayName("Should delete all transactions atomically")
  void deleteAll_ValidInput_DeletesAllTransactions() {
    var user = AppUser.builder().id(1L).build();
    var tx1 = Transaction.builder().id(10L).user(user).build();
    var tx2 = Transaction.builder().id(20L).user(user).build();
    given(transactionRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(tx1, tx2));

    service.deleteAll(user, List.of(10L, 20L));

    verify(transactionRepository).deleteAll(List.of(tx1, tx2));
  }

  @Test
  @DisplayName("Should reject batch delete when any transaction is missing")
  void deleteAll_MissingTransaction_ThrowsException() {
    var user = AppUser.builder().id(1L).build();
    var tx1 = Transaction.builder().id(10L).user(user).build();
    given(transactionRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(tx1));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.deleteAll(user, List.of(10L, 20L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Transaction not found");
    verify(transactionRepository, org.mockito.Mockito.never()).deleteAll(any());
  }

  @Test
  @DisplayName("Should reject batch delete when any transaction belongs to another user")
  void deleteAll_UnauthorizedTransaction_ThrowsException() {
    var user = AppUser.builder().id(1L).build();
    var otherUser = AppUser.builder().id(2L).build();
    var tx1 = Transaction.builder().id(10L).user(user).build();
    var tx2 = Transaction.builder().id(20L).user(otherUser).build();
    given(transactionRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(tx1, tx2));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.deleteAll(user, List.of(10L, 20L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not authorized to delete this transaction");
    verify(transactionRepository, org.mockito.Mockito.never()).deleteAll(any());
  }

  @Test
  @DisplayName("Should summarize transactions returned from repository")
  void summarize_DateRange_ReturnsSummary() {
    var user = AppUser.builder().id(1L).build();
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-06-01T00:00:00Z");
    var expense =
        Transaction.builder()
            .type(TransactionType.EXPENSE)
            .amount(30000L)
            .currency(Currency.VND)
            .build();
    var income =
        Transaction.builder()
            .type(TransactionType.INCOME)
            .amount(5000000L)
            .currency(Currency.VND)
            .build();
    given(transactionRepository.findByUserIdAndOccurredAtBetween(1L, from, to))
        .willReturn(List.of(expense, income));

    var result = service.summarize(user, from, to);

    assertThat(result.transactionCount()).isEqualTo(2);
    assertThat(result.totals().get(Currency.VND).totalExpense()).isEqualTo(30000L);
    assertThat(result.totals().get(Currency.VND).totalIncome()).isEqualTo(5000000L);
  }

  @Test
  @DisplayName("Should find transactions by user and preserve requested ID order")
  void findAllByUserAndIds_ValidInput_ReturnsOwnedTransactionsInRequestedOrder() {
    var user = AppUser.builder().id(1L).build();
    var otherUser = AppUser.builder().id(2L).build();
    var tx10 = Transaction.builder().id(10L).user(user).build();
    var tx20 = Transaction.builder().id(20L).user(user).build();
    var tx30 = Transaction.builder().id(30L).user(otherUser).build();
    given(transactionRepository.findAllById(List.of(20L, 10L, 30L)))
        .willReturn(List.of(tx10, tx30, tx20));

    var result = service.findAllByUserAndIds(user, List.of(20L, 10L, 30L));

    assertThat(result).containsExactly(tx20, tx10);
  }

  @Test
  @DisplayName("Should return empty list when finding by empty ID list")
  void findAllByUserAndIds_EmptyIds_ReturnsEmptyList() {
    var user = AppUser.builder().id(1L).build();

    var result = service.findAllByUserAndIds(user, List.of());

    assertThat(result).isEmpty();
    verify(transactionRepository, org.mockito.Mockito.never()).findAllById(any());
  }

  @Test
  @DisplayName("Should find latest transaction by user")
  void findLatestByUser_ValidUser_ReturnsLatestTransaction() {
    var user = AppUser.builder().id(1L).build();
    var tx = Transaction.builder().id(99L).user(user).build();
    given(transactionRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(1L))
        .willReturn(java.util.Optional.of(tx));

    var result = service.findLatestByUser(user);

    assertThat(result).contains(tx);
  }

  @Test
  @DisplayName("Should throw NullPointerException when user is null in record")
  void record_NullUser_ThrowsNpe() {
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE, 1L, Currency.VND, Category.FOOD, "test", null);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.record(null, parsed, MessageChannel.TELEGRAM, "test"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("user must not be null");
  }

  @Test
  @DisplayName("Should throw NullPointerException when parsed is null in record")
  void record_NullParsed_ThrowsNpe() {
    var user = AppUser.builder().id(1L).build();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.record(user, null, MessageChannel.TELEGRAM, "test"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("parsed must not be null");
  }

  @Test
  @DisplayName("Should throw NullPointerException when source is null in record")
  void record_NullSource_ThrowsNpe() {
    var user = AppUser.builder().id(1L).build();
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE, 1L, Currency.VND, Category.FOOD, "test", null);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.record(user, parsed, null, "test"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source must not be null");
  }

  @Test
  @DisplayName("Should throw NullPointerException when user is null in summarize")
  void summarize_NullUser_ThrowsNpe() {
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-06-01T00:00:00Z");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.summarize(null, from, to))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("user must not be null");
  }

  @Test
  @DisplayName("Should throw NullPointerException when from is null in summarize")
  void summarize_NullFrom_ThrowsNpe() {
    var user = AppUser.builder().id(1L).build();
    var to = Instant.parse("2026-06-01T00:00:00Z");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.summarize(user, null, to))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("from must not be null");
  }

  @Test
  @DisplayName("Should return zero summary when no transactions found")
  void summarize_EmptyResult_ReturnsZeroSummary() {
    var user = AppUser.builder().id(1L).build();
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-06-01T00:00:00Z");
    given(transactionRepository.findByUserIdAndOccurredAtBetween(1L, from, to))
        .willReturn(List.of());

    var result = service.summarize(user, from, to);

    assertThat(result.transactionCount()).isZero();
    assertThat(result.totals()).isEmpty();
  }
}
