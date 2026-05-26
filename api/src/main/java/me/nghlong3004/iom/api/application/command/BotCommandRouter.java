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
        for (BotCommandHandler handler : handlers) {
            if (handler.supports(message) && handler.handle(message)) {
                return;
            }
        }

        throw new IllegalStateException("No command handler handled the message");
    }
}
