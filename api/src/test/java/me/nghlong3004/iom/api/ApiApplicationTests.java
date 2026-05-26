package me.nghlong3004.iom.api;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.iom.api.domain.message.MessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
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
class ApiApplicationTests {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void contextLoads(ApplicationContext context) {
    assertThat(context).isNotNull();
  }

  @TestConfiguration
  static class TestMessageSenderConfig {

    @Bean
    MessageSender messageSender() {
      return message -> {};
    }
  }
}
