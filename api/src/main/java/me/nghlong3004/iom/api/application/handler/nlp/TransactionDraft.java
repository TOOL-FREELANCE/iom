package me.nghlong3004.iom.api.application.handler.nlp;

/**
 * Tool input DTO for a transaction parsed by the LLM.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 6/1/2026
 */
public record TransactionDraft(
    String type, long amount, String currency, String category, String note, String occurredAt) {}

