package me.nghlong3004.iom.api.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import me.nghlong3004.iom.api.domain.transaction.Currency;

/**
 * Currency-aware amount formatting utility.
 *
 * <p>Formats amounts stored in smallest currency units to human-readable strings:
 * <ul>
 *   <li>{@code format(30000, VND)} -> {@code "30.000đ"}</li>
 *   <li>{@code format(1050, USD)} -> {@code "$10.50"}</li>
 *   <li>{@code format(5000, JPY)} -> {@code "\u00A55,000"}</li>
 * </ul>
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public final class AmountFormatter {

  private AmountFormatter() {}

  public static String format(long amountInSmallestUnit, Currency currency) {
    double displayAmount = toDisplayAmount(amountInSmallestUnit, currency);
    var formatted = formatNumber(displayAmount, currency);
    return attachSymbol(formatted, currency);
  }

  private static double toDisplayAmount(long amountInSmallestUnit, Currency currency) {
    if (currency.getMinorUnits() == 0) {
      return amountInSmallestUnit;
    }
    return amountInSmallestUnit / Math.pow(10, currency.getMinorUnits());
  }

  private static String formatNumber(double amount, Currency currency) {
    var symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(currency.getGroupSeparator().charAt(0));
    symbols.setDecimalSeparator(currency.getGroupSeparator().equals(".") ? ',' : '.');

    String pattern =
        currency.getMinorUnits() > 0
            ? "#,##0." + "0".repeat(currency.getMinorUnits())
            : "#,##0";

    var formatter = new DecimalFormat(pattern, symbols);
    return formatter.format(amount);
  }

  private static String attachSymbol(String formatted, Currency currency) {
    return switch (currency) {
      case VND -> formatted + currency.getSymbol();
      case USD, EUR, GBP, JPY, KRW -> currency.getSymbol() + formatted;
    };
  }
}
