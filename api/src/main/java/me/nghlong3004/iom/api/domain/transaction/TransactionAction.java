package me.nghlong3004.iom.api.domain.transaction;

import java.util.Objects;

/**
 * User-requested management action for an existing transaction.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public sealed interface TransactionAction {

  /** Delete a transaction resolved from the supplied reference. */
  record Delete(TransactionReference reference) implements TransactionAction {

    public Delete {
      Objects.requireNonNull(reference, "reference is required");
    }
  }

  /** Update a transaction resolved from the supplied reference. */
  record Update(TransactionReference reference, UpdateFields changes) implements TransactionAction {

    public Update {
      Objects.requireNonNull(reference, "reference is required");
      Objects.requireNonNull(changes, "changes is required");
    }
  }

  /** Undo the last recorded transaction. */
  record Undo() implements TransactionAction {}

  /** Confirm the pending action in the current conversation. */
  record Confirm() implements TransactionAction {}

  /** Cancel the pending action in the current conversation. */
  record Cancel() implements TransactionAction {}
}
