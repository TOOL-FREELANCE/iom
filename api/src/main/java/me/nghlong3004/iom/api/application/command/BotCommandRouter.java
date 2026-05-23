package me.nghlong3004.iom.api.application.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Component
@RequiredArgsConstructor
public class BotCommandRouter {

    private final List<BotCommandHandler> handlers;

    public void route(IncomingMessage message) {
        handlers.stream()
                .filter(handler -> handler.supports(message))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No command handler found"))
                .handle(message);
    }
}