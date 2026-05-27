package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.application.port.out.DateRangeResolver;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DateRangeResolverChain Unit Tests")
@ExtendWith(MockitoExtension.class)
class DateRangeResolverChainTest {

  @Mock private DateRangeResolver first;
  @Mock private DateRangeResolver second;

  @Test
  @DisplayName("Should return first resolver result when it matches")
  void resolve_FirstMatches_ReturnsFirst() {
    var range = sampleRange();
    given(first.resolve("hom nay")).willReturn(Optional.of(range));
    var chain = new DateRangeResolverChain(List.of(first, second));

    var result = chain.resolve("hom nay");

    assertThat(result).contains(range);
  }

  @Test
  @DisplayName("Should fallback to second resolver when first returns empty")
  void resolve_FirstEmpty_FallsBackToSecond() {
    var range = sampleRange();
    given(first.resolve("tu 1/5 den 20/5")).willReturn(Optional.empty());
    given(second.resolve("tu 1/5 den 20/5")).willReturn(Optional.of(range));
    var chain = new DateRangeResolverChain(List.of(first, second));

    var result = chain.resolve("tu 1/5 den 20/5");

    assertThat(result).contains(range);
  }

  @Test
  @DisplayName("Should return empty when all resolvers return empty")
  void resolve_AllEmpty_ReturnsEmpty() {
    given(first.resolve("hello")).willReturn(Optional.empty());
    given(second.resolve("hello")).willReturn(Optional.empty());
    var chain = new DateRangeResolverChain(List.of(first, second));

    var result = chain.resolve("hello");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should work with empty resolver list")
  void resolve_NoResolvers_ReturnsEmpty() {
    var chain = new DateRangeResolverChain(List.of());

    var result = chain.resolve("anything");

    assertThat(result).isEmpty();
  }

  private DateRange sampleRange() {
    return DateRange.custom(
        Instant.parse("2026-05-01T00:00:00Z"),
        Instant.parse("2026-05-02T00:00:00Z"),
        "Test");
  }
}
