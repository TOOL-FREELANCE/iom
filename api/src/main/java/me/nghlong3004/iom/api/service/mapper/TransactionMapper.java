package me.nghlong3004.iom.api.service.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.user.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", source = "user")
  @Mapping(target = "type", source = "parsed.type")
  @Mapping(target = "amount", source = "parsed.amount")
  @Mapping(target = "currency", source = "parsed.currency")
  @Mapping(target = "category", source = "parsed.category")
  @Mapping(target = "note", source = "parsed.note")
  @Mapping(target = "occurredAt", source = "parsed.occurredAt")
  @Mapping(target = "sourcePlatform", source = "source")
  @Mapping(target = "rawInput", source = "rawInput")
  Transaction toEntity(
      AppUser user, ParsedTransaction parsed, MessageChannel source, String rawInput);

  default Instant mapLocalDateToInstant(LocalDate date) {
    if (date == null) {
      return Instant.now();
    }
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
}
