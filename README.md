# IOM — Input Output Money

A personal finance assistant that turns natural language chat messages into structured income and expense records.

> Send a message like `ăn sáng 30k` — IOM parses it, saves the transaction, and replies with a confirmation. No forms, no friction.

---

## Features

- **Telegram Bot** — Record transactions by chatting naturally in Vietnamese
- **Smart Parsing** — Automatically detects amount, type (income/expense), and category
- **Persistent Storage** — All transactions saved to PostgreSQL
- **Clean Architecture** — Platform-independent core; Telegram is just one channel adapter
- **Daily/Monthly Summary** — Quick spending overview via bot commands
- **Docker Ready** — One-command deployment with Docker Compose

---

## Architecture

IOM follows a **Clean Architecture-lite / Hexagonal Architecture-lite** style. The core business logic is completely independent from external platforms.

```
External Channel (Telegram, Web, Zalo, ...)
    ↓
Channel Adapter
    ↓
Application Use Cases
    ↓
Domain Model
    ↓
Repository / Infrastructure
```

Adding a new channel (Web, Zalo, Facebook) requires only a new adapter — no changes to core logic.

---

## Tech Stack

| Layer         | Technology                                   |
|---------------|----------------------------------------------|
| Language      | Java 21 (Virtual Threads)                    |
| Framework     | Spring Boot 4.0                              |
| Database      | PostgreSQL 16                                |
| Migration     | Flyway                                       |
| ORM           | Spring Data JPA                              |
| Bot Platform  | Telegram (Long Polling)                      |
| Bot Library   | [TelegramBots](https://github.com/rubenlagus/TelegramBots) 9.6.0 |
| Monitoring    | Spring Boot Actuator + Prometheus            |
| Build Tool    | Maven                                        |
| Containerization | Docker + Docker Compose                   |

---

## Project Structure

```
iom/
├── api/                          # Spring Boot backend
│   ├── src/main/java/me/nghlong3004/iom/api/
│   │   ├── ApiApplication.java
│   │   ├── config/               # App configuration (Telegram properties, etc.)
│   │   ├── channel/
│   │   │   └── telegram/         # Telegram adapter (bot, dispatcher, mapper, reply sender)
│   │   ├── application/
│   │   │   ├── command/          # Bot command handlers (start, help, echo, router)
│   │   │   └── usecase/          # Application use cases
│   │   └── domain/
│   │       ├── message/          # IncomingMessage, OutgoingMessage, MessageSender
│   │       └── MessageChannel.java
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── client/                       # React web app (planned for later phases)
│
├── docker-compose.yml
├── .env.example
├── .env.prod.example
├── BRIEF.md                      # Detailed product & architecture brief
└── README.md
```

---

## Getting Started

### Prerequisites

- **Java 21** or later
- **Maven 3.9+**
- **PostgreSQL 16** (or use Docker)
- A **Telegram Bot Token** from [@BotFather](https://t.me/BotFather)

### 1. Clone the repository

```bash
git clone https://github.com/nghlong3004/iom.git
cd iom
```

### 2. Set up environment variables

```bash
cp .env.example .env
```

Edit `.env` and fill in your values:

```env
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username_here
POSTGRES_DB=iom_dev
POSTGRES_USER=iom
POSTGRES_PASSWORD=iom
```

### 3. Run with Docker Compose (recommended)

```bash
docker compose up -d
```

This starts both the **API** and **PostgreSQL** containers. The API will be available at `http://localhost:8080`.

### 4. Run locally (without Docker)

Make sure PostgreSQL is running locally, then:

```bash
cd api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> **Windows:** Use `mvnw.cmd` instead of `./mvnw`.

---

## Usage

Once the bot is running, open Telegram and start chatting:

| Message               | What IOM does                              |
|-----------------------|--------------------------------------------|
| `/start`              | Welcome message                            |
| `/help`               | Show available commands                    |
| `ăn sáng 30k`        | Records 30,000₫ expense for breakfast      |
| `đổ xăng 50k`        | Records 50,000₫ expense for fuel           |
| `lương tháng này 5tr` | Records 5,000,000₫ income (salary)        |
| `mua sách 120k hôm qua` | Records 120,000₫ expense from yesterday |

Bot replies with a confirmation:

```
Đã ghi nhận: Chi 30.000đ cho ăn sáng.
```

---

## Configuration

### Profiles

| Profile | Purpose                           |
|---------|-----------------------------------|
| `dev`   | Local development with verbose logging, SQL logs, Telegram long polling |
| `prod`  | Production with env-based secrets, minimal logging, Flyway migrations  |

### Environment Variables

| Variable                   | Description                      | Default     |
|----------------------------|----------------------------------|-------------|
| `SPRING_PROFILES_ACTIVE`   | Active Spring profile            | `dev`       |
| `API_PORT`                 | API server port                  | `8080`      |
| `POSTGRES_DB`              | PostgreSQL database name         | `iom_dev`   |
| `POSTGRES_USER`            | PostgreSQL username              | `iom`       |
| `POSTGRES_PASSWORD`        | PostgreSQL password              | `iom`       |
| `DB_POOL_MAX_SIZE`         | HikariCP max pool size           | `5`         |
| `DB_POOL_MIN_IDLE`         | HikariCP min idle connections    | `1`         |
| `TELEGRAM_BOT_ENABLED`     | Enable/disable Telegram bot      | `true`      |
| `TELEGRAM_BOT_TOKEN`       | Telegram bot token from BotFather| —           |
| `TELEGRAM_BOT_USERNAME`    | Telegram bot username            | —           |

---

## Roadmap

### Phase 1 — MVP (Current)

- [x] Telegram echo bot
- [x] Command handler refactor (`/start`, `/help`, unknown, echo)
- [ ] Simple Vietnamese money parser
- [ ] Save transactions to PostgreSQL
- [ ] Confirmation reply messages
- [ ] Daily/monthly summary (`/today`, `/month`)

### Phase 2 — Enhanced

- [ ] Edit & delete recent transactions
- [ ] Basic category suggestion
- [ ] REST API for web client
- [ ] Excel export

### Phase 3 — Advanced

- [ ] Image receipt parsing & OCR
- [ ] AI-based natural language parsing
- [ ] Web dashboard (React)
- [ ] Account linking (Telegram ↔ Web)
- [ ] Multi-platform support (Zalo, Facebook)
- [ ] Budget warnings & recurring transactions

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## Author

**Nguyen Hoang Long** — [@nghlong3004](https://github.com/nghlong3004)
