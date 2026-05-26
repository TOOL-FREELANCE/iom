package me.nghlong3004.iom.api.application.port.out;

import java.util.Optional;
import me.nghlong3004.iom.api.domain.summary.ParsedSummaryIntent;

/**
 * Interprets natural-language requests for transaction summaries.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/26/2026
 */
public interface SummaryIntentInterpreter {

  Optional<ParsedSummaryIntent> interpret(String text);
}
