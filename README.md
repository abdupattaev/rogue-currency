# RogueProject — Telegram Currency Bot (CBU rates)

A simple Java Telegram bot that shows exchange rates from the Central Bank of Uzbekistan (CBU) and converts amounts between foreign currencies and UZS.

Built with:
- Java (requires JDK 21+; project is configured for Java 25 target in Maven)
- TelegramBots library
- Jackson (for JSON)
- Maven + Shade plugin (fat JAR)

## Features
- Fetches live currency rates from CBU public API
- Commands and free-text parsing (Uzbek prompts):
  - /start — short intro and help
  - /kurs — show USD→UZS rate (default)
    - /kurs EUR — show EUR→UZS
    - /kurs 120 EUR — convert 120 EUR to UZS
    - /kurs 200000 UZS USD — convert 200000 so'm to USD
  - /list — show available currencies
  - /namuna — show usage examples
- Free text works too: "120" (assumes USD), "RUB", "EUR 120", "200000 UZS USD"

## Configuration
The bot reads configuration from environment variables or Java system properties.

Required:
- TELEGRAM_BOT_TOKEN or BOT_TOKEN — your bot token from @BotFather

Optional:
- TELEGRAM_BOT_USERNAME or BOT_USERNAME or -Dtelegram.bot.username=... — bot username (defaults to CurrencyUzb_bot)

Main reads the first non-blank value among the variables below:
- Token: TELEGRAM_BOT_TOKEN, BOT_TOKEN, -Dtelegram.bot.token
- Username: TELEGRAM_BOT_USERNAME, BOT_USERNAME, -Dtelegram.bot.username (fallback: CurrencyUzb_bot)

## Build
Prerequisites:
- JDK 21 or newer installed (project targets Java 25 in pom.xml; Java 21+ works fine with Maven toolchains set accordingly)
- Maven 3.9+

Build the fat JAR:

```powershell
# From project root
mvn -q -e -DskipTests package
```
The executable JAR will be in target/ as RogueProject-1.0-SNAPSHOT-shaded.jar (exact name may vary).

## Run
Provide your Telegram bot token via environment variable or JVM system property.

Windows PowerShell:
```powershell
$env:TELEGRAM_BOT_TOKEN = "123456:ABC-DEF..."
# Optional
$env:TELEGRAM_BOT_USERNAME = "YourBotUserName"

java -jar target/RogueProject-1.0-SNAPSHOT-shaded.jar
```

Or via JVM properties:
```powershell
java -Dtelegram.bot.token="123456:ABC-DEF..." -Dtelegram.bot.username="YourBotUserName" -jar target/RogueProject-1.0-SNAPSHOT-shaded.jar
```

Linux/macOS (bash/zsh):
```bash
export TELEGRAM_BOT_TOKEN="123456:ABC-DEF..."
export TELEGRAM_BOT_USERNAME="YourBotUserName"
java -jar target/RogueProject-1.0-SNAPSHOT-shaded.jar
```

If token is missing, the app exits with a helpful message.

## Usage (inside Telegram)
- Send /start to get help.
- Send /list to see all available currency codes (CCY) from CBU.
- Send /kurs for USD→UZS. You can specify a CCY and/or amount:
  - /kurs EUR
  - /kurs 120 EUR
  - /kurs 200000 UZS USD
- You can also just type:
  - 120           → 120 USD to UZS
  - EUR           → EUR→UZS rate
  - EUR 120       → 120 EUR to UZS
  - 200000 UZS USD → 200k so'm to USD

The bot replies in Uzbek with formatted numbers and brief hints.

## Data source
- CBU public API endpoint used by default: https://cbu.uz/ru/arkhiv-kursov-valyut/json/
- HTTP client: java.net.http.HttpClient
- JSON: Jackson

## Project structure
- org.example.Main — application entry point, registers the bot
- org.example.MyBot — Telegram long-polling bot, handles commands and free-text
- org.example.currency.CbuClient — tiny HTTP client for CBU API
- org.example.currency.CurrencyService — business logic, formatting, conversions
- org.example.currency.CurrencyRate — DTO mapped from CBU response

## Troubleshooting
- Bot does not start: ensure TELEGRAM_BOT_TOKEN is set (or -Dtelegram.bot.token provided).
- 401/403 on Telegram: token invalid or bot blocked.
- CBU API errors: temporary network issues; check logs and try again.
- Windows PowerShell quoting: prefer double quotes for values with special characters.

## License
This project doesn’t declare a license. If you plan to open source it, consider adding a LICENSE file (e.g., MIT).
