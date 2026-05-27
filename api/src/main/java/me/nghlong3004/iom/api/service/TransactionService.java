package me.nghlong3004.iom.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.repository.TransactionRepository;
import me.nghlong3004.iom.api.service.mapper.TransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  @Transactional
  public Transaction record(
      AppUser user, ParsedTransaction parsed, MessageChannel source, String rawInput) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(parsed, "parsed must not be null");
    Objects.requireNonNull(source, "source must not be null");

    var transaction = transactionMapper.toEntity(user, parsed, source, rawInput);
    var saved = transactionRepository.save(transaction);

    log.info(
        "Transaction recorded: userId={}, type={}, amount={}, currency={}, category={}",
        user.getId(),
        parsed.type(),
        parsed.amount(),
        parsed.currency(),
        parsed.category());

    return saved;
  }

  public TransactionSummary summarize(AppUser user, Instant from, Instant to) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");

    var transactions = transactionRepository.findByUserIdAndOccurredAtBetween(user.getId(), from, to);
    return TransactionSummary.from(transactions);
  }

  public List<Transaction> findByRange(
      AppUser user, me.nghlong3004.iom.api.domain.summary.DateRange dateRange) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(dateRange, "dateRange must not be null");
    return transactionRepository.findByUserIdAndOccurredAtBetween(
        user.getId(), dateRange.from(), dateRange.to());
  }
}
