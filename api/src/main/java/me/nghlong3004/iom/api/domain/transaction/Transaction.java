package me.nghlong3004.iom.api.domain.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.iom.api.domain.MessageChannel;
import me.nghlong3004.iom.api.domain.user.AppUser;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Entity
@Table(name = "transactions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TransactionType type;

  @Column(nullable = false)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 5)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Category category;

  @Column(length = 500)
  private String note;

  @Column(columnDefinition = "TEXT")
  private String rawInput;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageChannel sourcePlatform;

  @Column(nullable = false)
  private Instant occurredAt;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  /**
   * Applies partial changes from the given {@link UpdateFields}. Only non-null fields are updated.
   */
  public void applyChanges(UpdateFields changes) {
    if (changes.amount() != null) {
      this.amount = changes.amount();
    }
    if (changes.category() != null) {
      this.category = changes.category();
    }
    if (changes.note() != null) {
      this.note = changes.note();
    }
    if (changes.type() != null) {
      this.type = changes.type();
    }
  }
}
