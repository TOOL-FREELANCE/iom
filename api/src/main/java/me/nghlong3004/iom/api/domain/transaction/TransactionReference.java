package me.nghlong3004.iom.api.domain.transaction;

import java.util.Objects;

/**
 * Describes how a user refers to an existing transaction in a chat message.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public sealed interface TransactionReference {

  /** The most recently recorded transaction in the current conversation. */
  record Latest() implements TransactionReference {}

  /** A 1-based index from the most recently viewed transaction list. */
  record ByIndex(int index) implements TransactionReference {

    public ByIndex {
      if (index < 1) {
        throw new IllegalArgumentException("index must be >= 1");
      }
    }
  }

  /** A natural-language description to match against recent transactions. */
  record ByMatch(String description) implements TransactionReference {

    public ByMatch {
      Objects.requireNonNull(description, "description is required");
      if (description.isBlank()) {
        throw new IllegalArgumentException("description is required");
      }
    }
  }
}
