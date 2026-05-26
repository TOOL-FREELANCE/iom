package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TransactionService Integration Tests")
class TransactionServiceIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private AppUserRepository appUserRepository;
  @Autowired private TransactionService transactionService;

  @Test
  @DisplayName("Should persist transaction and summarize it by date range")
  void recordAndSummarize_ValidTransaction_PersistsAndSummarizes() {
    var user = appUserRepository.save(AppUser.builder().build());
    var occurredAt = LocalDate.of(2026, 5, 26);
    var parsed =
        new ParsedTransaction(
            TransactionType.EXPENSE,
            30000L,
            Currency.VND,
            Category.FOOD,
            "an sang",
            occurredAt);

    var saved = transactionService.record(user, parsed, MessageChannel.TELEGRAM, "an sang 30k");
    var zone = ZoneId.systemDefault();
    var from = occurredAt.atStartOfDay(zone).toInstant();
    var to = occurredAt.plusDays(1).atStartOfDay(zone).toInstant();

    var summary = transactionService.summarize(user, from, to);

    assertThat(saved.getId()).isNotNull();
    assertThat(summary.transactionCount()).isEqualTo(1);
    assertThat(summary.totals().get(Currency.VND).totalExpense()).isEqualTo(30000L);
    assertThat(summary.totals().get(Currency.VND).totalIncome()).isZero();
  }

  @TestConfiguration
  static class TestMessageSenderConfig {

    @Bean
    MessageSender messageSender() {
      return message -> {};
    }
  }
}
