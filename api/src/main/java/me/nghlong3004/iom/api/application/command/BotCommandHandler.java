package me.nghlong3004.iom.api.application.command;

import me.nghlong3004.iom.api.domain.message.IncomingMessage;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
public interface BotCommandHandler {

  boolean supports(IncomingMessage message);

  boolean handle(IncomingMessage message);
}
