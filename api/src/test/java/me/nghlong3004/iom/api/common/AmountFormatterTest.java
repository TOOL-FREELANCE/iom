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

    assertThat(result).isEqualTo("30.000đ");
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

  @Test
  @DisplayName("Should format EUR with decimal places and prefix")
  void format_Eur_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(2599L, Currency.EUR);

    assertThat(result).isEqualTo("\u20AC25.99");
  }

  @Test
  @DisplayName("Should format GBP with decimal places and prefix")
  void format_Gbp_ReturnsFormattedAmount() {
    var result = AmountFormatter.format(100L, Currency.GBP);

    assertThat(result).isEqualTo("\u00A31.00");
  }

  @Test
  @DisplayName("Should format zero amount VND")
  void format_ZeroAmountVnd_ReturnsZero() {
    var result = AmountFormatter.format(0L, Currency.VND);

    assertThat(result).isEqualTo("0đ");
  }

  @Test
  @DisplayName("Should format large VND amount with correct grouping")
  void format_LargeAmountVnd_GroupsSeparatorsCorrectly() {
    var result = AmountFormatter.format(1000000000L, Currency.VND);

    assertThat(result).isEqualTo("1.000.000.000đ");
  }
}
