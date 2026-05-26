package me.nghlong3004.iom.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.iom.api.domain.transaction.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
@DisplayName("AmountFormatter Unit Tests")
class AmountFormatterTest {

  @Test
  @DisplayName("Should format VND with dot grouping and suffix")
  void format_Vnd_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(30000L, Currency.VND);

    assertThat(result).isEqualTo("30.000d");
  }

  @Test
  @DisplayName("Should format USD cents with decimal places and prefix")
  void format_Usd_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(1050L, Currency.USD);

    assertThat(result).isEqualTo("$10.50");
  }

  @Test
  @DisplayName("Should format JPY without fractional units")
  void format_Jpy_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(5000L, Currency.JPY);

    assertThat(result).isEqualTo("\u00A55,000");
  }

  @Test
  @DisplayName("Should format KRW without fractional units")
  void format_Krw_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(7000L, Currency.KRW);

    assertThat(result).isEqualTo("\u20A97,000");
  }
}
