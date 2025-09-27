package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            String token = firstNonBlank(
                    System.getenv("TELEGRAM_BOT_TOKEN"),
                    System.getenv("BOT_TOKEN"),
                    System.getProperty("telegram.bot.token")
            );
            String username = firstNonBlank(
                    System.getenv("TELEGRAM_BOT_USERNAME"),
                    System.getenv("BOT_USERNAME"),
                    System.getProperty("telegram.bot.username"),
                    "CurrencyUzb_bot"
            );

            if (token == null || token.isBlank()) {
                System.err.println("TELEGRAM_BOT_TOKEN yoki BOT_TOKEN aniqlanmadi. Iltimos, tokenni muhit o'zgaruvchisi sifatida bering.");
                System.err.println("Masalan (PowerShell): $env:TELEGRAM_BOT_TOKEN='123:ABC' ; java -jar app.jar");
                System.exit(1);
            }

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot(username, token));
            System.out.println("MyBot started as @" + username + ". Press Ctrl+C to stop.");
        } catch (TelegramApiException e) {
            System.err.println("Failed to start MyBot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
