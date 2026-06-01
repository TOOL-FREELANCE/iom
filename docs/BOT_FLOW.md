# Bot Flow And Intent Handling

This document is the quick handover for the current Telegram bot behavior.
Read this before opening the message handler classes.

## Current Scope

The bot currently supports:

- Recording one or more income/expense transactions from natural-language messages.
- Slash commands: `/start`, `/help`, `/today`, `/month`.
- Bot-name suffix normalization, for example `/today@my_bot`.
- Natural-language summary requests, for example:
  - `xem tong hom nay`
  - `thang nay chi bao nhieu`
  - `hom qua tieu bao nhieu`
  - `tu 1/5 den 20/5 chi bao nhieu`
- Transaction history / detail view, for example:
  - `hom qua mua gi`
  - `hom kia chi gi`
  - `lich su tuan nay`
- Transaction management:
  - `xoa cai vua roi`
  - `xoa so 2`
  - `undo`
  - `ok` / `huy` confirmation flow for pending delete actions
- Fallback guidance for unrelated text.
- Vietnamese user-facing replies via `messages.properties`.

Important current limitation: `TransactionService` and domain objects contain update support, but
the active NLP tool surface only exposes record, view, delete, and undo. Natural-language update
messages are not wired as a tool yet.

## High-Level Runtime Flow

```text
Telegram update
  -> TelegramUpdateDispatcher
  -> TelegramMessageMapper
  -> HandleIncomingMessageUseCase
  -> BotMessageRouter
  -> ordered BotMessageHandler chain
  -> MessageSender reply
```

The router iterates handlers in `@Order` priority. For each handler:

- If `supports(message)` returns `false`, the router skips it.
- If `supports(message)` returns `true`, the router calls `handle(message)`.
- `handle() == true`: routing stops.
- `handle() == false`: router continues to the next matching handler.

## Handler Order

| Order | Package | Handler | Purpose |
| --- | --- | --- | --- |
| `1` | `handler/command` | `StartCommandHandler` | `/start` welcome message |
| `2` | `handler/command` | `HelpCommandHandler` | `/help` usage message |
| `3` | `handler/command` | `SummaryCommandHandler` | `/today`, `/month` summary |
| `50` | `handler/nlp` | `NlpMessageHandler` | all non-command text through Spring AI Tool Calling |
| `98` | `handler/command` | `UnknownCommandHandler` | unknown slash commands |

There are no separate active `RecordTransactionHandler`, `ManageTransactionHandler`,
`ViewFinancesHandler`, or `EchoMessageHandler` classes anymore. They were replaced by
`NlpMessageHandler` plus `FinanceTools`.

## Slash Commands

Key files:

- `application/handler/command/BotCommand.java`
- `application/handler/command/BotCommandParser.java`
- `application/handler/command/StartCommandHandler.java`
- `application/handler/command/HelpCommandHandler.java`
- `application/handler/command/SummaryCommandHandler.java`
- `application/handler/command/UnknownCommandHandler.java`

`BotCommandParser` normalizes commands and strips bot-name suffixes, so `/today@my_bot` is treated
as `/today`.

`SummaryCommandHandler` handles all summary slash commands by reading date range factories from
`BotCommand`. To add `/week`, add a `BotCommand` enum constant with a `DateRange` factory.

## NLP Tool-Calling Flow

Key files:

- `application/handler/nlp/NlpMessageHandler.java`
- `application/handler/nlp/NlpSystemPromptFactory.java`
- `application/handler/nlp/PendingActionHandler.java`
- `application/handler/nlp/FinanceTools.java`
- `application/handler/nlp/FinanceToolsFactory.java`
- `application/handler/nlp/TransactionDraft.java`
- `application/handler/nlp/TransactionDraftMapper.java`
- `config/ChatClientConfig.java`
- `service/TransactionService.java`
- `common/ConfirmationFormatter.java`
- `common/FinanceViewRenderer.java`
- `common/BotMessages.java`

Flow:

```text
NlpMessageHandler.handle(message)
  1. Load ConversationContext by key: channel + ":" + externalUserId
  2. If context is AWAITING_CONFIRMATION:
     - exact confirm keyword -> execute pending delete, clear pending, reply
     - exact cancel keyword -> clear pending, reply
     - anything else -> clear pending and continue to LLM
  3. Resolve AppUser through UserResolver
  4. Create request-scoped FinanceTools through FinanceToolsFactory
  5. Build ChatClient with system prompt + user text + tools
  6. Spring AI calls the selected tool or returns a fallback-style Vietnamese answer
  7. Send returned content through MessageSender
```

The keyword guard exists so short confirmation messages like `ok` and `huy` do not go through the
LLM and accidentally get interpreted as unrelated text.

## Available Finance Tools

`FinanceTools` is created per request. It is not a Spring component because it carries the current
user, message channel, raw input, and conversation key.

| Tool | Purpose | Side effects |
| --- | --- | --- |
| `recordTransactions` | Saves one or more income/expense items parsed by the model | records all items atomically, saves bounded `lastRecordedTransactionIds`, returns batch confirmation |
| `recordTransaction` | Compatibility single-item tool | delegates to `recordTransactions` with one item |
| `viewFinances` | Renders summary or transaction history for an LLM-supplied date range | saves bounded `lastViewedTransactionIds` only for indexed detail/compact views |
| `deleteTransaction` | Starts a delete confirmation flow for `LATEST` or `BY_INDEX` references | saves pending `DELETE`, returns confirmation prompt |
| `undoLastTransaction` | Starts confirmation for deleting the last record action | keeps ids pending until user confirms |

### Record Transactions

```text
recordTransactions([TransactionDraft...])
  -> reject empty or >10 item batches
  -> TransactionDraftMapper maps every draft to ParsedTransaction
  -> TransactionService.recordAll(user, parsedTransactions, channel, rawInput)
  -> context.lastRecordedTransactionIds = saved ids (bounded to 10)
  -> ConfirmationFormatter.formatBatch(parsedTransactions)
```

Amounts must be in the smallest currency unit. For VND this is the plain dong amount; for USD/EUR/GBP
this means cents. The batch path is all-or-nothing: invalid arguments prevent any item from being
saved.

### View Finances

```text
viewFinances(from, to, label, filter, viewMode)
  -> DateRange.custom(fromInstant, toInstant, label)
  -> parse FlowFilter and ViewMode
  -> TransactionService.findByRange(user, dateRange)
  -> TransactionSummary.from(transactions)
  -> autoAdjustViewMode(mode, count)
  -> if effective mode is DETAIL/COMPACT, context.lastViewedTransactionIds = displayed ids
  -> FinanceViewRenderer.render(...)
```

The LLM supplies the resolved `from` and `to` dates directly as `yyyy-MM-dd`. There is no active
`DateRangeResolver` chain in the current code.

### Delete And Confirmation

```text
deleteTransaction(referenceType, index)
  -> LATEST uses the last id from context.lastRecordedTransactionIds
  -> BY_INDEX uses context.lastViewedTransactionIds[index - 1]
  -> find transaction for current user
  -> context.setPending(DELETE, transactionId, description)
  -> ask user to type ok/huy

ok
  -> PendingActionHandler keyword guard
  -> TransactionService.delete(user, pending.transactionId)
  -> context.clearPending()
  -> reply deleted

huy
  -> context.clearPending()
  -> reply cancelled
```

## Conversation Context

Key files:

- `domain/conversation/ConversationContext.java`
- `application/port/out/ConversationContextStore.java`
- `service/InMemoryConversationContextStore.java`

State machine:

```text
IDLE --deleteTransaction--> AWAITING_CONFIRMATION
AWAITING_CONFIRMATION --ok--> execute delete --> IDLE
AWAITING_CONFIRMATION --huy--> IDLE
AWAITING_CONFIRMATION --new non-keyword message--> clear pending, process new message
```

Context is updated by:

- `FinanceTools.recordTransactions`: saves bounded `lastRecordedTransactionIds`
- `FinanceTools.viewFinances`: saves bounded `lastViewedTransactionIds` for indexed views
- `FinanceTools.deleteTransaction`: saves pending `DELETE`
- `PendingActionHandler`: executes or clears pending actions

`InMemoryConversationContextStore` is an in-memory adapter with TTL behavior. It is suitable for the
current bot flow, but pending state is not durable across application restarts.

## Finance Rendering

Key files:

- `domain/summary/DateRange.java`
- `domain/summary/ViewMode.java`
- `domain/summary/FlowFilter.java`
- `common/FinanceViewRenderer.java`
- `service/TransactionSummary.java`

`FinanceViewRenderer` supports:

- `SUMMARY`: totals only.
- `DETAIL`: individual transactions only.
- `COMPACT`: individual transactions plus totals.

Renderer output is plain text by default. Domain categories do not carry emoji, so the same rendered
content is suitable for Telegram, Web, and future channels.

View mode auto-adjustment inside `FinanceTools`:

| Requested | Transaction Count | Effective |
| --- | --- | --- |
| SUMMARY | any | SUMMARY |
| DETAIL | 0 | DETAIL |
| DETAIL | 1-10 | COMPACT |
| DETAIL | >10 | SUMMARY |

`FlowFilter` controls the totals/detail focus:

- `ALL`: show both expense and income.
- `EXPENSE`: show expenses only.
- `INCOME`: show income only.

## Configuration

DeepSeek/Spring AI config lives in profile YAML files:

```yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: ${DEEPSEEK_MODEL:deepseek-v4-flash}
          temperature: 0
          max-tokens: ${DEEPSEEK_MAX_TOKENS:512}
```

There is no active `iom.bot.intents` keyword configuration in the current source tree. Confirmation
keywords are currently constants in `PendingActionHandler`.

## User-Facing Messages

All Vietnamese bot replies should go through:

- `common/BotMessages.java`
- `src/main/resources/messages.properties`

Do not hardcode Vietnamese user-facing text in Java classes. Prompt/schema text inside LLM adapters
or tool descriptions may stay in Java because it is not sent directly as a bot reply.

## Test Map

Useful tests:

- `BotMessageRouterTest`: routing continues on `false` and stops on `true`.
- `BotCommandParserTest`: slash command normalization, including bot-name suffix.
- `BasicCommandHandlerTest`: start, help, unknown command behavior.
- `SummaryCommandHandlerTest`: unified `/today` and `/month` handler.
- `NlpMessageHandlerTest`: non-command support, pending-handler short-circuit, LLM delegation.
- `PendingActionHandlerTest`: confirm/cancel pending delete flow.
- `FinanceToolsTest`: batch record, view, delete, undo tool behavior.
- `TransactionDraftMapperTest`: tool DTO to domain mapping.
- `FinanceViewRendererTest`: SUMMARY, DETAIL, COMPACT output.
- `DateRangeTest`: factory methods and validation.
- `FinanceQueryTest`: sealed variants construction.
- `TransactionActionTest` and `TransactionReferenceTest`: domain transaction management records.
- `ConversationContextTest`: state transitions and index resolution.
- `InMemoryConversationContextStoreTest`: create, save, isolation.

Verification command:

```powershell
cd api
.\mvnw.cmd test
```

Integration tests using Testcontainers may skip when Docker Desktop is unavailable.
