package me.nghlong3004.iom.api.domain.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Getter
@RequiredArgsConstructor
public enum Category {
  FOOD("🍜"),
  TRANSPORT("🚗"),
  SALARY("💰"),
  EDUCATION("📚"),
  SHOPPING("🛒"),
  ENTERTAINMENT("🎬"),
  HEALTH("🏥"),
  HOUSING("🏠"),
  OTHER("📌");

  private final String emoji;
}
