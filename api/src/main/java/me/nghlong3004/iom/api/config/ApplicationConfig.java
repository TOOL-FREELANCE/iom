package me.nghlong3004.iom.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Configuration
public class ApplicationConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
  }
}
