package me.nghlong3004.iom.api.domain.summary;

/**
 * Controls the level of detail in finance view responses.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/27/2026
 */
public enum ViewMode {
  /** Show only aggregated totals by currency. */
  SUMMARY,

  /** Show individual transaction list. */
  DETAIL,

  /** Show individual transaction list followed by aggregated totals. */
  COMPACT
}
