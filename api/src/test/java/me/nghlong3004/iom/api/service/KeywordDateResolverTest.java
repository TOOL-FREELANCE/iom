package me.nghlong3004.iom.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.nghlong3004.iom.api.config.BotIntentProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KeywordDateResolver Unit Tests")
class KeywordDateResolverTest {

  private final BotIntentProperties properties =
      new BotIntentProperties(
          new BotIntentProperties.Summary(
              List.of("xem", "tong", "bao nhieu"),
              List.of("hom nay", "today"),
              List.of("hom qua", "yesterday"),
              List.of("hom kia"),
              List.of("tuan nay", "week"),
              List.of("thang nay", "month"),
              List.of("chi", "tieu"),
              List.of("thu", "luong"),
              List.of("mua gi", "chi gi", "lich su")));

  private final KeywordDateResolver resolver = new KeywordDateResolver(properties);

  @Test
  @DisplayName("Should resolve 'hom nay' to today")
  void resolve_HomNay_ReturnsToday() {
    var result = resolver.resolve("xem tong hom nay");
    assertThat(result).isPresent();
    assertThat(result.get().label()).isEqualTo("Hôm nay");
  }

  @Test
  @DisplayName("Should resolve 'hom qua' to yesterday")
  void resolve_HomQua_ReturnsYesterday() {
    var result = resolver.resolve("hom qua tieu bao nhieu");
    assertThat(result).isPresent();
    assertThat(result.get().label()).isEqualTo("Hôm qua");
  }

  @Test
  @DisplayName("Should resolve 'hom kia' to 2 days ago")
  void resolve_HomKia_ReturnsDaysAgo() {
    var result = resolver.resolve("hom kia chi gi");
    assertThat(result).isPresent();
    assertThat(result.get().label()).isEqualTo("Hôm kia");
  }

  @Test
  @DisplayName("Should resolve 'tuan nay' to this week")
  void resolve_TuanNay_ReturnsThisWeek() {
    var result = resolver.resolve("tuan nay tieu bao nhieu");
    assertThat(result).isPresent();
    assertThat(result.get().label()).isEqualTo("Tuần này");
  }

  @Test
  @DisplayName("Should resolve 'thang nay' to this month")
  void resolve_ThangNay_ReturnsThisMonth() {
    var result = resolver.resolve("thang nay chi bao nhieu");
    assertThat(result).isPresent();
    assertThat(result.get().label()).startsWith("Tháng ");
  }

  @Test
  @DisplayName("Should return empty for unrelated text")
  void resolve_UnrelatedText_ReturnsEmpty() {
    var result = resolver.resolve("hello world");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty for null text")
  void resolve_NullText_ReturnsEmpty() {
    var result = resolver.resolve(null);
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty for blank text")
  void resolve_BlankText_ReturnsEmpty() {
    var result = resolver.resolve("   ");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle Vietnamese accented text")
  void resolve_AccentedText_Resolves() {
    var result = resolver.resolve("hôm qua tiêu bao nhiêu");
    assertThat(result).isPresent();
    assertThat(result.get().label()).isEqualTo("Hôm qua");
  }
}
