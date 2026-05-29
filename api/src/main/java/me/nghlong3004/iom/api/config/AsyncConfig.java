package me.nghlong3004.iom.api.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

  @Bean(name = "emailTaskExecutor")
  public Executor emailTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean(name = "scanTaskExecutor")
  public Executor scanTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
