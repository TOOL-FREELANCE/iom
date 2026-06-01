package me.nghlong3004.iom.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Exposes a {@link ChatClient.Builder} bean for NLP message handling via Spring AI Tool Calling.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/29/2026
 */
@Configuration
public class ChatClientConfig {

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
    return ChatClient.builder(chatModel);
  }
}
