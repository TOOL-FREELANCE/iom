package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;
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
}
