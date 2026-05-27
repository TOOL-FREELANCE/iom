# Bot Flow And Intent Handling

This document is the quick handover for the current Telegram bot behavior.
Read this before opening the command handler classes.

## Current Scope

The bot currently supports:

- Recording income/expense transactions from natural-language messages.
- Slash commands: `/start`, `/help`, `/today`, `/month`.
- Bot-name suffix normalization, for example `/today@my_bot`.
- Natural-language summary requests, for example:
  - `xem tong hom nay`
  - `thang nay chi bao nhieu`
  - `hom qua tieu bao nhieu`
  - `tu 1/5 den 20/5 chi bao nhieu`
- **Transaction history / detail view**, for example:
  - `hom qua mua gi`
  - `hom kia chi gi`
  - `lich su tuan nay`
- Fallback guidance for unrelated text.
- Vietnamese user-facing replies via `messages.properties`.

## High-Level Runtime Flow

```text
Telegram update
  -> TelegramUpdateDispatcher
  -> TelegramMessageMapper
  -> HandleIncomingMessageUseCase
  -> BotCommandRouter
  -> ordered BotCommandHandler chain
  -> MessageSender reply
```

The router calls every handler whose `supports(message)` returns `true`.
Each handler returns a boolean from `handle(message)`:

- `true`: message was handled; routing stops.
- `false`: handler did not handle it; router tries the next handler.

This matters because non-command text first goes through transaction parsing. If the
transaction parser says "not a transaction", routing continues to the view finances handler
and then fallback.

## Handler Order

| Order | Handler | Purpose |
| --- | --- | --- |
| `1` | `StartCommandHandler` | `/start` welcome message |
| `2` | `HelpCommandHandler` | `/help` usage message |
| `3` | `TodaySummaryHandler` | `/today` command summary |
| `4` | `MonthSummaryHandler` | `/month` command summary |
| `10` | `UnknownCommandHandler` | unknown slash commands |
| `50` | `RecordTransactionHandler` | non-command transaction recording |
| `80` | `ViewFinancesHandler` | natural-language summary/history intent |
| `99` | `EchoMessageHandler` | fallback guidance |

## Transaction Recording

Key files:

- `application/port/out/MessageInterpreter.java`
- `service/LlmMessageInterpreter.java`
- `application/command/RecordTransactionHandler.java`
- `service/TransactionService.java`
- `common/ConfirmationFormatter.java`

Flow:

```text
RecordTransactionHandler
  -> MessageInterpreter.interpret(text)
  -> Optional.empty(): return false
  -> ParsedTransaction: resolve user, save transaction, send confirmation, return true
```

`LlmMessageInterpreter` uses Spring AI `ChatModel` with DeepSeek configured by Spring AI.
It returns empty for blank input, non-transaction JSON, malformed JSON, invalid values, or
model call failures.

## Finance View Pipeline (Summary / History)

Key files:

- `domain/summary/DateRange.java` — value object for date ranges
- `domain/summary/ViewMode.java` — SUMMARY, DETAIL, COMPACT
- `domain/summary/FlowFilter.java` — ALL, EXPENSE, INCOME
- `domain/summary/FinanceQuery.java` — sealed interface: View | Clarification
- `application/port/out/DateRangeResolver.java` — port for date resolution
- `service/KeywordDateResolver.java` — deterministic keyword matcher (Order 1)
- `service/LlmDateRangeResolver.java` — LLM fallback (Order 2)
- `service/DateRangeResolverChain.java` — chain orchestrator
- `application/command/ViewFinancesHandler.java` — pipeline handler
- `common/FinanceViewRenderer.java` — multi-mode renderer
- `config/BotIntentProperties.java` — keyword config

### Pipeline Architecture

```text
ViewFinancesHandler.handle(message)
  1. DateRangeResolverChain.resolve(text)     — Chain of Responsibility
     ├─ KeywordDateResolver (Order 1)          deterministic: "hom qua" → yesterday
     └─ LlmDateRangeResolver (Order 2)        LLM fallback: "tu 1/5 den 20/5"
  2. detectFlowFilter(text)                   — keyword-based: chi → EXPENSE
  3. detectViewMode(text)                     — keyword-based: "mua gi" → DETAIL
  4. TransactionService.findByRange()         — fetch data
  5. autoAdjustViewMode(mode, count)          — DETAIL + ≤10 → COMPACT, >10 → SUMMARY
  6. FinanceViewRenderer.render()             — Strategy per ViewMode
```

### Date Resolution (Chain of Responsibility)

`KeywordDateResolver` handles common Vietnamese date expressions deterministically:

| Keyword | Resolves To |
| --- | --- |
| `hom nay`, `today` | `DateRange.today()` |
| `hom qua`, `yesterday` | `DateRange.yesterday()` |
| `hom kia` | `DateRange.daysAgo(2)` |
| `tuan nay`, `week` | `DateRange.thisWeek()` |
| `thang nay`, `month` | `DateRange.thisMonth()` |

When keyword matching fails, `LlmDateRangeResolver` calls DeepSeek for complex date
expressions like `"tu 1/5 den 20/5"` or `"7 ngay qua"`.

### ViewMode Auto-Adjustment

| Requested | Transaction Count | Effective |
| --- | --- | --- |
| SUMMARY | any | SUMMARY |
| DETAIL | 0 | DETAIL (shows empty message) |
| DETAIL | 1–10 | COMPACT (list + totals) |
| DETAIL | >10 | SUMMARY (totals only) |

### FlowFilter

`FlowFilter` controls reply focus:

- `ALL`: show both expense and income totals.
- `EXPENSE`: show expense total only.
- `INCOME`: show income total only.

Command summaries keep `ALL` by default. Natural-language summaries can use any filter.

## Configuration

Keyword config lives in profile YAML files under:

```yaml
iom:
  bot:
    intents:
      summary:
        action-keywords: [...]
        today-keywords: [...]
        yesterday-keywords: [...]
        day-before-keywords: [...]
        this-week-keywords: [...]
        month-keywords: [...]
        expense-keywords: [...]
        income-keywords: [...]
        detail-keywords: [...]
```

DeepSeek/Spring AI config uses:

```properties
spring.ai.deepseek.api-key=${DEEPSEEK_API_KEY:}
spring.ai.deepseek.base-url=${DEEPSEEK_BASE_URL:https://api.deepseek.com}
spring.ai.deepseek.chat.options.model=${DEEPSEEK_MODEL:deepseek-v4-flash}
spring.ai.deepseek.chat.options.temperature=0
spring.ai.deepseek.chat.options.max-tokens=${DEEPSEEK_MAX_TOKENS:512}
```

## User-Facing Messages

All Vietnamese bot replies should go through:

- `common/BotMessages.java`
- `src/main/resources/messages.properties`

Do not hardcode Vietnamese user-facing text in Java classes. Prompt/schema text inside LLM
adapters may stay in Java because it is not sent directly as a bot reply.

## Test Map

Useful tests:

- `BotCommandRouterTest`: routing continues on `false` and stops on `true`.
- `BotCommandParserTest`: slash command normalization, including bot-name suffix.
- `RecordTransactionHandlerTest`: transaction parse empty vs valid result.
- `ViewFinancesHandlerTest`: pipeline mock — date resolution, mode detection, rendering.
- `KeywordDateResolverTest`: all keyword combinations, accent handling.
- `DateRangeResolverChainTest`: chain ordering, fallback behavior.
- `FinanceViewRendererTest`: SUMMARY, DETAIL, COMPACT output.
- `DateRangeTest`: factory methods, validation.
- `FinanceQueryTest`: sealed variants construction.
- `LlmMessageInterpreterTest`: DeepSeek transaction JSON parsing.

Verification command:

```powershell
cd api
.\mvnw.cmd test
```

Integration tests using Testcontainers may skip when Docker Desktop is unavailable.
