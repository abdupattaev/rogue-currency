package org.example;

import org.example.currency.CurrencyService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = Logger.getLogger(MyBot.class.getName());
    private final CurrencyService currencyService = new CurrencyService();

    private final String botUsername;
    private final String botToken;

    public MyBot(String botUsername, String botToken) {
        this.botUsername = botUsername != null && !botUsername.isBlank() ? botUsername : "CurrencyUzb_bot";
        this.botToken = botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUserName() : "unknown";
        LOGGER.info(() -> "Keldi: chat=" + chatId + ", user=" + username + ", text='" + text + "'");

        if (text.equalsIgnoreCase("/start") || text.toLowerCase().startsWith("/start@")) {
            sendHtml(chatId, "Assalomu alaykum! Men valyuta kurslarini ko'rsataman.\n" +
                    "Foydalanish:\n" +
                    "• <b>/kurs</b> — USD→UZS kursi\n" +
                    "• <b>/list</b> — mavjud valyutalar ro'yxati\n" +
                    "• <b>/namuna</b> — qanday ishlashini ko'rsatuvchi qisqa namunalar\n" +
                    "• Shuningdek, oddiy yozishingiz ham mumkin: masalan <code>120</code> (USD bo'yicha hisoblayman), <code>RUB</code> (RUB kursi), yoki <code>EUR 120</code> (120 EUR ni so'mda hisoblayman).");
            return;
        }

        if (text.startsWith("/kurs")) {
            handleKursCommand(chatId, text);
            return;
        }

        if (text.startsWith("/list")) {
            sendHtml(chatId, currencyService.listCurrenciesMessage());
            return;
        }

        if (text.startsWith("/namuna") || text.startsWith("/samples")) {
            sendHtml(chatId, samplesMessage());
            return;
        }

        // Ozod matn sifatida: raqam yoki CCY yoki raqam + CCY
        handleFreeText(chatId, text);
    }

    private void handleKursCommand(Long chatId, String text) {
        try {
            // /kurs@BotName ... ni tozalaymiz
            String cmd = text.split("\\s+")[0];
            if (cmd.contains("@")) {
                text = text.replaceFirst("/kurs@[^\\s]+", "/kurs");
            }
            String[] parts = text.split("\\s+");
            String ccy = null;
            Double amount = null;
            boolean isUzsInput = false;

            if (parts.length == 1) {
                // faqat /kurs → USD kursi
                sendHtml(chatId, currencyService.getUsdToUzsMessage());
                return;
            }

            // Qolgan tokenlarni tahlil qilamiz
            for (int i = 1; i < parts.length; i++) {
                String p = parts[i].trim();
                if (p.isEmpty()) continue;
                if (isUzsToken(p)) {
                    isUzsInput = true;
                } else if (isNumber(p)) {
                    amount = parseNumber(p);
                } else if (isCcyToken(p)) {
                    String t = normalizeCcy(p);
                    if ("UZS".equalsIgnoreCase(t)) {
                        isUzsInput = true;
                    } else {
                        ccy = t;
                    }
                }
            }

            if (amount == null && ccy == null) {
                sendHtml(chatId, currencyService.getUsdToUzsMessage());
            } else if (amount == null) {
                sendHtml(chatId, currencyService.getRateMessage(ccy));
            } else { // amount bor
                if (isUzsInput) {
                    String target = (ccy != null ? ccy : "USD");
                    String info = "Hisoblayapman: " + formatAmount(amount) + " so'mni " + target + "ga hisoblash...";
                    sendHtml(chatId, info);
                    sendHtml(chatId, currencyService.getReverseConversionMessage(ccy, amount));
                } else {
                    String info = "Hisoblayapman: " + formatAmount(amount) + " " + (ccy != null ? ccy : "USD") + " ni so'mga hisoblash...";
                    sendHtml(chatId, info);
                    sendHtml(chatId, currencyService.getConversionMessage(ccy, amount));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Kurs buyruqni qayta ishlashda xato", e);
            sendHtml(chatId, "⚠️ Buyruqni tushunmadim. Namuna: <b>/kurs</b> yoki <code>120</code>, <code>USD 120</code> kabi yozing. /list bilan ro'yxatni ko'ring.");
        }
    }

    private void handleFreeText(Long chatId, String text) {
        try {
            String[] parts = text.split("\\s+");
            String ccy = null;
            Double amount = null;
            boolean isUzsInput = false;

            for (String p : parts) {
                if (p.isBlank()) continue;
                if (isUzsToken(p)) {
                    isUzsInput = true;
                } else if (isNumber(p)) {
                    amount = parseNumber(p);
                } else if (isCcyToken(p)) {
                    String t = normalizeCcy(p);
                    if ("UZS".equalsIgnoreCase(t)) {
                        isUzsInput = true;
                    } else {
                        ccy = t;
                    }
                }
            }

            if (amount != null) {
                if (isUzsInput) {
                    String target = (ccy != null ? ccy : "USD");
                    String info = "Hisoblayapman: " + formatAmount(amount) + " so'mni " + target + "ga hisoblash...";
                    sendHtml(chatId, info);
                    sendHtml(chatId, currencyService.getReverseConversionMessage(ccy, amount));
                } else {
                    // Agar faqat son bo'lsa → USD bo'yicha hisob
                    String info = "Hisoblayapman: " + formatAmount(amount) + " " + (ccy != null ? ccy : "USD") + " ni so'mga hisoblash...";
                    sendHtml(chatId, info);
                    sendHtml(chatId, currencyService.getConversionMessage(ccy, amount));
                }
            } else if (ccy != null) {
                sendHtml(chatId, currencyService.getRateMessage(ccy));
            } else {
                // Qisqa yordam
                sendHtml(chatId, "Tushunmadim. Masalan, <code>/kurs</code>, <code>/list</code>, <code>120</code>, <code>USD</code> yoki <code>120 so'm</code> deb yozing.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Matnni tahlil qilishda xato", e);
            sendHtml(chatId, "⚠️ So'rovni qayta ishlashda xatolik yuz berdi.");
        }
    }

    private boolean isNumber(String s) {
        return s.matches("^-?\\d+(?:[.,]\\d+)?$");
    }

    private double parseNumber(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private boolean isCcyToken(String s) {
        String t = s.trim();
        return t.matches("^[A-Za-z]{3}$") || t.matches("^\\d{3}$");
    }

    private String normalizeCcy(String s) {
        String t = s.trim();
        if (t.matches("^[A-Za-z]{3}$")) return t.toUpperCase(Locale.ROOT);
        return t; // numeric code
    }

    private boolean isUzsToken(String s) {
        String t = s.trim();
        return t.matches("(?i)^(UZS|so['’`]?m|som|sum)$");
    }

    private String formatAmount(double amount) {
        java.text.DecimalFormat df = (java.text.DecimalFormat) java.text.DecimalFormat.getInstance(new java.util.Locale("uz", "UZ"));
        df.applyPattern("#,##0.00");
        return df.format(amount);
    }

    // Qisqa namunalar xabari
    private String samplesMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Qisqa namunalar:</b>\n");
        sb.append("1) <code>USD 120</code> — 120 USD ni so'mga hisoblayman.\n");
        sb.append("2) <code>UZS 333000</code> — 300 000 so'mni USDga hisoblayman.\n");
        sb.append("3) <code>UZS 120000 RUB</code> — 120 000 so'mni RUBga hisoblayman.\n\n");
        sb.append("Shuningdek: <code>120</code> (default USD), <code>EUR</code> (EUR kursi), <code>50 EUR</code> yoki <code>EUR 50</code>.\n");
        sb.append("Ko'proq valyutalar uchun <b>/list</b> ni ko'ring.");
        return sb.toString();
    }

    private void sendHtml(Long chatId, String html) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(html);
        sm.setParseMode("HTML");
        sm.disableWebPagePreview();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, "Xabar yuborilmadi", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
