package me.nghlong3004.iom.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.iom.api.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  List<Transaction> findByUserIdAndOccurredAtBetween(Long userId, Instant from, Instant to);

  Optional<Transaction> findFirstByUserIdOrderByOccurredAtDescIdDesc(Long userId);
}
