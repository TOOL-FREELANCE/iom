package me.nghlong3004.iom.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@ConfigurationProperties(prefix = "iom.telegram")
public record TelegramBotProperties(boolean enabled, String botToken, String botUsername) {}
