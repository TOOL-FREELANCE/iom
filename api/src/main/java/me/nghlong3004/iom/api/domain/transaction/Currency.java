package me.nghlong3004.iom.api.domain.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported currencies with formatting metadata.
 *
 * <p>{@code minorUnits} indicates how many digits represent fractional currency.
 * VND stores raw dong (minorUnits=0), USD stores cents (minorUnits=2, so 1050 means $10.50).
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Getter
@RequiredArgsConstructor
public enum Currency {
  VND("\u0111", ".", 0),
  USD("$", ",", 2),
  EUR("\u20AC", ",", 2),
  JPY("\u00A5", ",", 0),
  KRW("\u20A9", ",", 0),
  GBP("\u00A3", ",", 2);

  private final String symbol;
  private final String groupSeparator;
  private final int minorUnits;
}
