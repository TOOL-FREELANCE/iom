package me.nghlong3004.iom.api.application.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/23/2026
 */
@Getter
@RequiredArgsConstructor
public enum BotCommand {
  START("/start"),
  HELP("/help"),
  ;

  private final String command;
}
