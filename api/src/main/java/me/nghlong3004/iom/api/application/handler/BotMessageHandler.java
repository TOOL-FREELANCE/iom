package me.nghlong3004.iom.api.application.handler;

import me.nghlong3004.iom.api.domain.message.IncomingMessage;

/**
 * Strategy interface for handling incoming bot messages. Implementations are ordered by {@link
 * org.springframework.core.annotation.Order} and routed by {@link BotMessageRouter}.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
public interface BotMessageHandler {

  boolean supports(IncomingMessage message);

  boolean handle(IncomingMessage message);
}
