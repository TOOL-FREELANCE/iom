# IOM (Input Output Money) Project Architecture & Guide

> Purpose: context handover for the current implementation. Keep this file and
> [`docs/BOT_FLOW.md`](BOT_FLOW.md) in sync after behavior or architecture changes.

## Read Order

- Read this file for the broad project architecture and module inventory.
- Read [`docs/BOT_FLOW.md`](BOT_FLOW.md) first when working on Telegram routing, bot replies,
  natural-language finance behavior, or bot handler tests.

## 1. Project Overview

- **Project Name**: IOM (Input Output Money)
- **Goal**: A multi-channel personal finance assistant. Users record incomes and expenses by sending
  natural-language chat messages such as `an sang 30k` or `lunch $12`.
- **Primary Channel**: Telegram Bot.
- **Future Channels**: Web dashboard and other chat platforms are planned.
- **Design Philosophy**: Core business logic is platform-independent. External channels such as
  Telegram are adapters around domain/application logic.

## 2. Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21, virtual threads enabled by profile config |
| Framework | Spring Boot 4.0.6 |
| LLM | Spring AI 2.0.0-M7 with DeepSeek chat model |
| Database | PostgreSQL 17 (`postgres:17-alpine`) |
| ORM & Migration | Spring Data JPA and Flyway |
| Object Mapping | MapStruct 1.6.3 |
| Bot Integration | Telegram Long Polling (`telegrambots-springboot-longpolling-starter` 9.6.0) |
| Security | Spring Security, OAuth2 Client, OAuth2 Resource Server, JWT |
| Build | Maven wrapper at `api/mvnw.cmd` |
| Containerization | Docker and Docker Compose |
| i18n | Spring MessageSource (`messages.properties`) |

## 3. Architecture

The project follows Hexagonal Architecture with strategy-style message handlers, request-scoped AI
tools, and small domain value objects.

### 3.1 Layer Diagram

```text
channel/telegram/                 External adapter
        |
application/usecase/              Use case orchestration
        |
application/handler/              Message routing
  |-- command/                    Slash commands
  |-- nlp/                        Free-text NLP through Spring AI Tool Calling
        |
        |-----------------------------.
        |                             |
domain/                         application/port/out/
  message/                         UserResolver
  transaction/                     ConversationContextStore
  summary/
  conversation/
  user/
        |
service/                         Infrastructure and business services
  DefaultUserResolver
  InMemoryConversationContextStore
  TransactionService
  TransactionSummary
  mapper/
        |
repository/                      Spring Data JPA repositories
```

**Dependency Rule**: `domain/` stays independent from `channel/`, `service/`, and `repository/`.
Ports live in `application/port/out/`. Adapters live in `service/` or `channel/`.

### 3.2 Telegram Message Flow

```text
TelegramBot
  -> TelegramUpdateDispatcher
  -> TelegramMessageMapper
  -> HandleIncomingMessageUseCase
  -> BotMessageRouter
  -> command handler or NlpMessageHandler
  -> MessageSender / TelegramReplySender
```

For a free-text finance message:

```text
NlpMessageHandler
  -> load ConversationContext
  -> PendingActionHandler handles pending ok/huy confirmations
  -> UserResolver resolves or provisions AppUser
  -> FinanceToolsFactory creates request-scoped FinanceTools
  -> ChatClient prompt from NlpSystemPromptFactory with tools
  -> Spring AI invokes record/view/delete/undo tool or returns fallback text
  -> TelegramReplySender sends the reply
```

### 3.3 Message Handler Chain

Handlers are ordered with `@Order`. `BotMessageRouter` stops at the first handler where
`supports(message)` and `handle(message)` both return `true`.

| Order | Package | Handler | Matches |
| --- | --- | --- | --- |
| 1 | `handler/command` | `StartCommandHandler` | `/start` |
| 2 | `handler/command` | `HelpCommandHandler` | `/help` |
| 3 | `handler/command` | `SummaryCommandHandler` | `/today`, `/month` |
| 50 | `handler/nlp` | `NlpMessageHandler` | Non-command text |
| 98 | `handler/command` | `UnknownCommandHandler` | Any unrecognized `/command` |

The older split NLP handlers (`RecordTransactionHandler`, `ManageTransactionHandler`,
`ViewFinancesHandler`, `EchoMessageHandler`) are no longer present. Their responsibilities now live
in `NlpMessageHandler` and `FinanceTools`.

## 4. Database Schema

Flyway migrations live in `api/src/main/resources/db/migration/`.

### app_users (V1)

| Column | Type | Notes |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| email | VARCHAR(255) UNIQUE | Nullable for Telegram-created users |
| password_hash | VARCHAR(255) | Nullable for OAuth/Telegram users |
| first_name | VARCHAR(35) | |
| last_name | VARCHAR(20) | |
| avatar_url | TEXT | |
| auth_provider | VARCHAR(20) | Enum: `AuthProvider` |
| role | VARCHAR(20) | Enum: `Role {USER, ADMIN}` |
| is_active | BOOLEAN | |
| created_at / updated_at | TIMESTAMPTZ | |

### external_accounts (V2)

| Column | Type | Notes |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| user_id | BIGINT FK -> app_users | |
| platform | VARCHAR(20) | Enum: `MessageChannel {TELEGRAM}` |
| external_user_id | VARCHAR(100) | Telegram user ID, etc. |
| display_name | VARCHAR(100) | |
| linked_at | TIMESTAMPTZ | |
| unique constraint | | `(platform, external_user_id)` |

### transactions (V3)

| Column | Type | Notes |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| user_id | BIGINT FK -> app_users | |
| type | VARCHAR(10) | Enum: `TransactionType {INCOME, EXPENSE}` |
| amount | BIGINT | Smallest currency unit |
| currency | VARCHAR(5) | Enum: `Currency {VND, USD, EUR, JPY, KRW, GBP}` |
| category | VARCHAR(30) | Enum: `Category` |
| note | VARCHAR(500) | Extracted description |
| raw_input | TEXT | Original user message |
| source_platform | VARCHAR(20) | Source channel |
| occurred_at | TIMESTAMPTZ | Transaction time |
| created_at / updated_at | TIMESTAMPTZ | |

Index: `idx_tx_user_occurred ON transactions(user_id, occurred_at)`.

## 5. Key Design Decisions

| Decision | Rationale |
| --- | --- |
| **Spring AI Tool Calling for NLP** | The LLM chooses among typed tools (`recordTransactions`, `recordTransaction`, `viewFinances`, `deleteTransaction`, `undoLastTransaction`) instead of a custom JSON parser/resolver pipeline. |
| **Batch transaction recording** | A single NLP message can record up to 10 transactions atomically via `recordTransactions`. |
| **Single NLP handler** | `NlpMessageHandler` handles all non-command text and delegates finance operations to `FinanceTools`. This avoids ordering bugs between separate NLP handlers. |
| **Keyword confirmation guard** | `ok` / `huy` are handled before the LLM when a delete action is awaiting confirmation. |
| **Request-scoped finance tools** | `FinanceTools` is created per message with the resolved user, channel, raw input, and conversation key. |
| **Bot handlers return boolean** | Routing can stop or continue based on each handler's result. |
| **Unified summary handler** | `SummaryCommandHandler` uses `BotCommand` date range factories for `/today` and `/month`. |
| **ConversationContext** | Tracks bounded `lastRecordedTransactionIds`, bounded `lastViewedTransactionIds`, and pending delete confirmation state. |
| **Finance renderer** | `FinanceViewRenderer` centralizes plain-text SUMMARY, DETAIL, and COMPACT bot replies. |
| **ViewMode auto-adjustment** | Detail requests with 1-10 transactions render as COMPACT; more than 10 render as SUMMARY. |
| **Multi-currency from day one** | `Currency` stores symbol, grouping, and minor-unit metadata. |
| **MapStruct mapping** | Cross-layer entity mapping uses MapStruct (`TransactionMapper`, `UserMapper`). |
| **Auto-provisioning users** | `DefaultUserResolver` creates an `AppUser` and `ExternalAccount` on first Telegram message. |
| **Externalized user-facing messages** | Vietnamese bot replies live in `messages.properties` and are accessed through `BotMessages`. |

Current limitation: domain and service update support exists (`TransactionAction.Update`,
`UpdateFields`, `TransactionService.update`), but natural-language update is not exposed through
`FinanceTools` yet.

## 6. File Inventory

### 6.1 Channel Adapter (`channel/telegram/`)

| File | Purpose |
| --- | --- |
| `TelegramBot.java` | Registers the Telegram long-polling bot |
| `TelegramUpdateDispatcher.java` | Filters valid text updates |
| `TelegramMessageMapper.java` | MapStruct mapper from Telegram `Update` to `IncomingMessage` |
| `TelegramReplySender.java` | Sends replies via Telegram client |

### 6.2 Application Layer (`application/`)

| File | Purpose |
| --- | --- |
| `HandleIncomingMessageUseCase.java` | Orchestrates incoming message handling |
| `BotMessageHandler.java` | Handler strategy interface |
| `BotMessageRouter.java` | Routes messages through ordered handlers |
| `BotCommand.java` | Enum for recognized slash commands |
| `BotCommandParser.java` | Normalizes slash commands and strips bot-name suffixes |
| `StartCommandHandler.java` | `/start` |
| `HelpCommandHandler.java` | `/help` |
| `SummaryCommandHandler.java` | `/today`, `/month` |
| `UnknownCommandHandler.java` | Unknown slash commands |
| `NlpMessageHandler.java` | Non-command text through Spring AI Tool Calling |
| `NlpSystemPromptFactory.java` | Builds the tool-calling system prompt |
| `PendingActionHandler.java` | Handles pending ok/huy confirmation replies |
| `FinanceTools.java` | Tool methods invoked by the LLM |
| `FinanceToolsFactory.java` | Creates request-scoped `FinanceTools` |
| `TransactionDraft.java` | Tool input DTO for transaction drafts |
| `TransactionDraftMapper.java` | MapStruct mapper from tool DTO to `ParsedTransaction` |
| `UserResolver.java` | Port: `IncomingMessage -> AppUser` |
| `ConversationContextStore.java` | Port: context storage |

### 6.3 Domain (`domain/`)

| Area | Files |
| --- | --- |
| Message | `IncomingMessage`, `OutgoingMessage`, `MessageSender` |
| Channel | `MessageChannel` |
| Summary | `DateRange`, `ViewMode`, `FlowFilter`, `FinanceQuery` |
| Transaction | `Transaction`, `ParsedTransaction`, `TransactionType`, `Category`, `Currency`, `UpdateFields`, `TransactionReference`, `TransactionAction` |
| Conversation | `ConversationContext` |
| User | `AppUser`, `ExternalAccount`, `Role` |

### 6.4 Service (`service/`)

| File | Purpose |
| --- | --- |
| `DefaultUserResolver.java` | Resolves/provisions users from external accounts |
| `InMemoryConversationContextStore.java` | In-memory conversation context store |
| `TransactionService.java` | Record single/batch, summarize, find, delete, update transactions |
| `TransactionSummary.java` | Multi-currency summary model |
| `CustomUserDetailsService.java` | Spring Security user details adapter |
| `mapper/TransactionMapper.java` | MapStruct transaction mapper |
| `mapper/UserMapper.java` | MapStruct user/external-account mapper |

### 6.5 Repository (`repository/`)

| File | Purpose |
| --- | --- |
| `AppUserRepository.java` | User lookup |
| `ExternalAccountRepository.java` | External account lookup |
| `TransactionRepository.java` | Transaction queries |

### 6.6 Common Utilities (`common/`)

| File | Purpose |
| --- | --- |
| `AmountFormatter.java` | Currency-aware amount formatting |
| `ConfirmationFormatter.java` | Record confirmation replies |
| `FinanceViewRenderer.java` | SUMMARY/DETAIL/COMPACT finance replies |
| `BotMessages.java` | Typed access to `messages.properties` |

### 6.7 Config (`config/`)

| File | Purpose |
| --- | --- |
| `ApplicationConfig.java` | Password encoder and ObjectMapper |
| `AsyncConfig.java` | Virtual-thread executors |
| `ChatClientConfig.java` | Prototype `ChatClient.Builder` for tool calling |
| `TelegramBotProperties.java` | Telegram bot configuration properties |
| `SecurityConfig.java` | CSRF, CORS, OAuth2, JWT extraction |
| `JwtConfig.java` | JWT encoder/decoder beans |
| `JwtAuthenticationEntryPoint.java` | Security error handling |

## 7. Configuration

Base profile selection lives in `application.yaml`; dev/prod details live in
`application-dev.yml` and `application-prod.yml`.

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

iom:
  telegram:
    enabled: ${TELEGRAM_BOT_ENABLED:true}
    bot-token: ${TELEGRAM_BOT_TOKEN:}
    bot-username: ${TELEGRAM_BOT_USERNAME:}
```

There is no active `iom.bot.intents` keyword configuration in the current code. Confirmation
keywords are constants in `PendingActionHandler`.

## 8. Coding Conventions

- Java files include author/since headers.
- Prefer constructor injection via `@RequiredArgsConstructor`; dependencies should be `final`.
- Use MapStruct for cross-layer entity mapping.
- Use records for DTOs/value objects/config properties, not JPA entities.
- JPA entities use Lombok getters/builders/constructors, not `@Data`.
- Services default to `@Transactional(readOnly = true)` and mark write methods with
  `@Transactional`.
- Return `Optional<T>` instead of `null` for absent values.
- User-facing Vietnamese bot text belongs in `messages.properties`, accessed through `BotMessages`.

## 9. How To Run

### Docker Compose

```bash
cp .env.example .env
# Fill in TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, DB and security settings
docker compose up -d
```

### Local Development

```powershell
cd api
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

## 10. Tests

Useful test groups:

- Handler tests: `BotMessageRouterTest`, `BasicCommandHandlerTest`, `SummaryCommandHandlerTest`,
  `BotCommandParserTest`, `NlpMessageHandlerTest`, `PendingActionHandlerTest`, `FinanceToolsTest`,
  `TransactionDraftMapperTest`
- Domain tests: `DateRangeTest`, `FinanceQueryTest`, `TransactionActionTest`,
  `TransactionReferenceTest`, `ConversationContextTest`
- Service tests: `TransactionServiceTest`, `TransactionSummaryTest`,
  `InMemoryConversationContextStoreTest`, `DefaultUserResolverTest`
- Common tests: `AmountFormatterTest`, `BotMessagesTest`, `ConfirmationFormatterTest`,
  `FinanceViewRendererTest`
- Integration tests use Testcontainers and may skip when Docker Desktop is unavailable.

Verification command:

```powershell
cd api
.\mvnw.cmd test
```

## 11. Remaining Work

| Item | Status | Details |
| --- | --- | --- |
| Telegram bot flow | Implemented | Commands and free-text NLP are wired. |
| Spring AI tool calling | Implemented | `NlpMessageHandler` registers `FinanceTools` with `ChatClient`. |
| Batch transaction recording | Implemented | `recordTransactions` stores up to 10 parsed items atomically. |
| Transaction history/detail view | Implemented | `viewFinances` renders summary/detail/compact views. |
| Delete and undo | Implemented | Delete uses confirmation; undo deletes the last recorded batch/action immediately. |
| Natural-language update | Not wired | Domain/service support exists, but no `FinanceTools` update method yet. |
| Durable conversation state | Not implemented | Current store is in-memory. |
| Web dashboard | Planned | OAuth/JWT foundations exist; no active client app in this repository. |
| OCR receipt scanning | Future | Mentioned in roadmap, not implemented in current source. |
