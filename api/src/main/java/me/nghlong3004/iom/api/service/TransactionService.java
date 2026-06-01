package me.nghlong3004.iom.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.summary.DateRange;
import me.nghlong3004.iom.api.domain.transaction.ParsedTransaction;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import me.nghlong3004.iom.api.domain.transaction.UpdateFields;
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

  @Transactional
  public List<Transaction> recordAll(
      AppUser user, List<ParsedTransaction> parsedTransactions, MessageChannel source, String rawInput) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(parsedTransactions, "parsedTransactions must not be null");
    Objects.requireNonNull(source, "source must not be null");

    if (parsedTransactions.isEmpty()) {
      throw new IllegalArgumentException("parsedTransactions must not be empty");
    }

    var transactions =
        parsedTransactions.stream()
            .map(parsed -> transactionMapper.toEntity(user, parsed, source, rawInput))
            .toList();
    var saved = transactionRepository.saveAll(transactions);

    log.info(
        "Transactions recorded: userId={}, count={}, source={}",
        user.getId(),
        saved.size(),
        source);

    return saved;
  }

  public TransactionSummary summarize(AppUser user, Instant from, Instant to) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");

    var transactions =
        transactionRepository.findByUserIdAndOccurredAtBetween(user.getId(), from, to);
    return TransactionSummary.from(transactions);
  }

  public List<Transaction> findByRange(AppUser user, DateRange dateRange) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(dateRange, "dateRange must not be null");
    return transactionRepository.findByUserIdAndOccurredAtBetween(
        user.getId(), dateRange.from(), dateRange.to());
  }

  public Optional<Transaction> findByUserAndId(AppUser user, Long transactionId) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(transactionId, "transactionId must not be null");
    return transactionRepository
        .findById(transactionId)
        .filter(transaction -> transaction.getUser().getId().equals(user.getId()));
  }

  @Transactional
  public void delete(AppUser user, Long transactionId) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(transactionId, "transactionId must not be null");

    var transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

    if (!transaction.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Not authorized to delete this transaction");
    }

    transactionRepository.delete(transaction);
    log.info("Transaction deleted: id={}, userId={}", transactionId, user.getId());
  }

  @Transactional
  public void deleteAll(AppUser user, List<Long> transactionIds) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(transactionIds, "transactionIds must not be null");

    if (transactionIds.isEmpty()) {
      throw new IllegalArgumentException("transactionIds must not be empty");
    }

    var transactions = transactionRepository.findAllById(transactionIds);
    if (transactions.size() != transactionIds.size()) {
      throw new IllegalArgumentException("Transaction not found");
    }

    var hasUnauthorizedTransaction =
        transactions.stream()
            .anyMatch(transaction -> !transaction.getUser().getId().equals(user.getId()));
    if (hasUnauthorizedTransaction) {
      throw new IllegalArgumentException("Not authorized to delete this transaction");
    }

    transactionRepository.deleteAll(transactions);
    log.info("Transactions deleted: count={}, userId={}", transactions.size(), user.getId());
  }

  @Transactional
  public Transaction update(AppUser user, Long transactionId, UpdateFields changes) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(transactionId, "transactionId must not be null");
    Objects.requireNonNull(changes, "changes must not be null");

    var transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

    if (!transaction.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Not authorized to update this transaction");
    }

    transaction.applyChanges(changes);
    var saved = transactionRepository.save(transaction);
    log.info("Transaction updated: id={}, userId={}", transactionId, user.getId());
    return saved;
  }
}
