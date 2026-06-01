package me.nghlong3004.iom.api.application.handler.nlp;

import java.time.LocalDate;
import me.nghlong3004.iom.api.domain.transaction.Category;
import me.nghlong3004.iom.api.domain.transaction.Currency;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.TransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps Spring AI tool transaction arguments into validated domain value objects.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 6/1/2026
 */
@Mapper(componentModel = "spring")
public interface TransactionDraftMapper {

  @Mapping(target = "type", source = "type")
  @Mapping(target = "amount", source = "amount")
  @Mapping(target = "currency", source = "currency")
  @Mapping(target = "category", source = "category")
  @Mapping(target = "note", source = "note")
  @Mapping(target = "occurredAt", source = "occurredAt")
  ParsedTransaction toParsed(TransactionDraft draft);

  default TransactionType mapTransactionType(String value) {
    return parseEnum(TransactionType.class, value);
  }

  default Currency mapCurrency(String value) {
    return parseEnum(Currency.class, value);
  }

  default Category mapCategory(String value) {
    return parseEnum(Category.class, value);
  }

  default LocalDate mapLocalDate(String value) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
      return null;
    }
    return LocalDate.parse(value);
  }

  private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(enumType.getSimpleName() + " is required");
    }
    return Enum.valueOf(enumType, value.trim().toUpperCase());
  }
}
