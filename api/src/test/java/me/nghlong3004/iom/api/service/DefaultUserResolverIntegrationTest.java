package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.message.MessageSender;
import me.nghlong3004.iom.api.repository.ExternalAccountRepository;
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
@DisplayName("DefaultUserResolver Integration Tests")
class DefaultUserResolverIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private DefaultUserResolver resolver;
  @Autowired private ExternalAccountRepository externalAccountRepository;

  @Test
  @DisplayName("Should auto-provision app user and external account for first Telegram message")
  void resolve_NewTelegramUser_CreatesUserAndExternalAccount() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "telegram-1", "chat-1", "/start");

    var result = resolver.resolve(message);

    assertThat(result.getId()).isNotNull();
    assertThat(
            externalAccountRepository.findByPlatformAndExternalUserId(
                MessageChannel.TELEGRAM, "telegram-1"))
        .isPresent()
        .get()
        .satisfies(
            account -> {
              assertThat(account.getUser().getId()).isEqualTo(result.getId());
              assertThat(account.getExternalUserId()).isEqualTo("telegram-1");
            });
  }

  @Test
  @DisplayName("Should reuse existing user when external account already exists")
  void resolve_ExistingTelegramUser_ReusesUser() {
    var message = new IncomingMessage(MessageChannel.TELEGRAM, "telegram-2", "chat-2", "/start");
    var first = resolver.resolve(message);

    var second = resolver.resolve(message);

    assertThat(second.getId()).isEqualTo(first.getId());
  }

  @TestConfiguration
  static class TestMessageSenderConfig {

    @Bean
    MessageSender messageSender() {
      return message -> {};
    }
  }
}
