package org.example.currency;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CurrencyService {
    private static final Logger LOGGER = Logger.getLogger(CurrencyService.class.getName());

    private final CbuClient client;

    public CurrencyService() {
        this.client = new CbuClient();
    }

    public CurrencyService(CbuClient client) {
        this.client = client;
    }

    /**
     * USD (840) uchun qisqa xabar.
     */
    public String getUsdToUzsMessage() {
        try {
            List<CurrencyRate> rates = client.fetchRates();
            Optional<CurrencyRate> usdOpt = findByCodeOrCcy(rates, "USD");
            if (usdOpt.isEmpty()) {
                return "USD (840) kursi CBU ma'lumotlarida topilmadi.";
            }
            CurrencyRate usd = usdOpt.get();
            return formatRateMessage(usd);
        } catch (IOException e) {
            return "⚠️ Xizmat vaqtincha mavjud emas: CBU bilan ulanishda muammo.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ So'rov bekor qilindi. Qayta urinib ko'ring.";
        } catch (Exception e) {
            return "⚠️ Kutilmagan xatolik yuz berdi. Keyinroq qayta urinib ko'ring.";
        }
    }

    /**
     * Istalgan CCY yoki uch raqamli kod uchun kurs xabari.
     */
    public String getRateMessage(String ccyOrCode) {
        String key = (ccyOrCode == null || ccyOrCode.isBlank()) ? "USD" : ccyOrCode.trim();
        try {
            List<CurrencyRate> rates = client.fetchRates();
            Optional<CurrencyRate> opt = findByCodeOrCcy(rates, key);
            if (opt.isEmpty()) {
                return "⚠️ '" + key + "' topilmadi. /list orqali mavjud kodlarni ko'ring.";
            }
            return formatRateMessage(opt.get());
        } catch (IOException e) {
            return "⚠️ Xizmat vaqtincha mavjud emas: CBU bilan ulanishda muammo.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ So'rov bekor qilindi. Qayta urinib ko'ring.";
        } catch (Exception e) {
            return "⚠️ Kutilmagan xatolik yuz berdi. Keyinroq qayta urinib ko'ring.";
        }
    }

    /**
     * Miqdorni UZSga hisoblash xabari. Ccy bo'sh bo'lsa USD olinadi.
     */
    public String getConversionMessage(String ccyOrCode, double amount) {
        String key = (ccyOrCode == null || ccyOrCode.isBlank()) ? "USD" : ccyOrCode.trim();
        try {
            List<CurrencyRate> rates = client.fetchRates();
            Optional<CurrencyRate> opt = findByCodeOrCcy(rates, key);
            if (opt.isEmpty()) {
                return "⚠️ '" + key + "' topilmadi. /list orqali mavjud kodlarni ko'ring.";
            }
            CurrencyRate r = opt.get();
            return formatConversionMessage(r, amount);
        } catch (IOException e) {
            return "⚠️ Xizmat vaqtincha mavjud emas: CBU bilan ulanishda muammo.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ So'rov bekor qilindi. Qayta urinib ko'ring.";
        } catch (Exception e) {
            return "⚠️ Kutilmagan xatolik yuz berdi. Keyinroq qayta urinib ko'ring.";
        }
    }

    /**
     * Mavjud valyutalar ro'yxati.
     */
    public String listCurrenciesMessage() {
        try {
            List<CurrencyRate> rates = client.fetchRates();
            // Unique by Ccy (alphabetic), keep the first occurrence by latest id
            Map<String, CurrencyRate> byCcy = rates.stream()
                    .filter(r -> r.getCcy() != null && !r.getCcy().isBlank())
                    .collect(Collectors.toMap(
                            r -> r.getCcy().toUpperCase(Locale.ROOT),
                            r -> r,
                            (a, b) -> {
                                Integer ai = a.getId();
                                Integer bi = b.getId();
                                if (ai == null) return b;
                                if (bi == null) return a;
                                return bi >= ai ? b : a; // keep latest id
                            },
                            LinkedHashMap::new
                    ));

            List<String> lines = byCcy.values().stream()
                    .sorted(Comparator.comparing(CurrencyRate::getCcy))
                    .map(r -> {
                        String name = preferredName(r);
                        String code = r.getCode();
                        int nominal = r.getNominalAsInt();
                        double rate = r.getRateAsDouble();
                        double perUnit = (nominal == 0) ? Double.NaN : rate / nominal;

                        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(new Locale("uz", "UZ"));
                        df.applyPattern("#,##0.00");

                        String rateStr = Double.isNaN(perUnit)
                                ? (r.getRate() != null ? r.getRate() : "")
                                : df.format(perUnit) + " so'm";

                        String suffix;
                        if (code != null && !code.isBlank()) {
                            suffix = rateStr.isBlank() ? " (" + code + ")" : " (" + code + ", " + rateStr + ")";
                        } else {
                            suffix = rateStr.isBlank() ? "" : " (" + rateStr + ")";
                        }

                        return "• <b>" + r.getCcy() + "</b> — " + name + suffix;
                    })
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("<b>Mavjud valyutalar ro'yxati:</b>\n");
            for (String line : lines) sb.append(line).append('\n');
            sb.append("\nMasalan, batafsil ma'lumot uchun <code>USD</code> yoki <code>RUB</code> deb yozing.\nNamuna uchun: <b>/namuna</b> ni ko'ring.");
            return sb.toString();
        } catch (IOException e) {
            return "⚠️ Xizmat vaqtincha mavjud emas: CBU bilan ulanishda muammo.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ So'rov bekor qilindi. Qayta urinib ko'ring.";
        } catch (Exception e) {
            return "⚠️ Kutilmagan xatolik yuz berdi. Keyinroq qayta urinib ko'ring.";
        }
    }

    // ===== Helpers & formatters =====

    private Optional<CurrencyRate> findByCodeOrCcy(List<CurrencyRate> rates, String key) {
        String k = key.trim();
        boolean isAlpha = k.matches("^[A-Za-z]{3}$");
        boolean isNumeric = k.matches("^\\d{3}$");

        return rates.stream()
                .filter(r -> (isAlpha && k.equalsIgnoreCase(r.getCcy())) || (isNumeric && k.equals(r.getCode())))
                .max(Comparator.comparing(CurrencyRate::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private String preferredName(CurrencyRate r) {
        return r.getNameUz() != null && !r.getNameUz().isBlank() ? r.getNameUz()
                : (r.getNameUzc() != null && !r.getNameUzc().isBlank() ? r.getNameUzc()
                : (r.getNameEn() != null && !r.getNameEn().isBlank() ? r.getNameEn() : r.getCcy()));
    }

    private String formatRateMessage(CurrencyRate r) {
        String name = preferredName(r);
        int nominal = r.getNominalAsInt();
        double rate = r.getRateAsDouble();
        double diff = r.getDiffAsDouble();
        String date = r.getDate();

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(new Locale("uz", "UZ"));
        df.applyPattern("#,##0.00");

        String rateStr = Double.isNaN(rate) ? r.getRate() : df.format(rate);
        String diffStr = Double.isNaN(diff) ? r.getDiff() : df.format(Math.abs(diff));
        String arrow = Double.isNaN(diff) ? "" : (diff > 0 ? "🔺" : (diff < 0 ? "🔻" : "➖"));
        String sign = Double.isNaN(diff) ? "" : (diff > 0 ? "+" : (diff < 0 ? "-" : ""));

        StringBuilder sb = new StringBuilder();
        sb.append("<b>Valyuta kursi:</b>\n");
        sb.append("<b>").append(r.getCcy()).append("</b> (").append(name).append(")\n");
        sb.append(nominal).append(" ").append(r.getCcy()).append(" = <b>").append(rateStr).append(" so'm</b>\n");
        if (arrow.length() > 0) {
            sb.append("O'zgarish: ").append(arrow).append(" ").append(sign).append(diffStr).append(" so'm\n");
        }
        if (date != null && !date.isBlank()) {
            sb.append("Sana: ").append(date);
        }
        return sb.toString();
    }

    private String formatConversionMessage(CurrencyRate r, double amount) {
        String name = preferredName(r);
        int nominal = r.getNominalAsInt();
        double rate = r.getRateAsDouble();
        String date = r.getDate();

        // 1 CCY uchun kurs
        double perUnit = (nominal == 0) ? Double.NaN : rate / nominal;

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(new Locale("uz", "UZ"));
        df.applyPattern("#,##0.00");

        String amountStr = df.format(amount);
        String perUnitStr = Double.isNaN(perUnit) ? r.getRate() : df.format(perUnit);
        String convertedStr = Double.isNaN(perUnit) ? "N/A" : df.format(amount * perUnit);

        StringBuilder sb = new StringBuilder();
        sb.append("<b>Hisob-kitob:</b>\n");
        sb.append(amountStr).append(" ").append(r.getCcy()).append(" (")
                .append('\"').append(name).append('\"').append(") = <b>")
                .append(convertedStr).append(" so'm</b>\n");
        sb.append("1 ").append(r.getCcy()).append(" = ").append(perUnitStr).append(" so'm\n");
        if (date != null && !date.isBlank()) {
            sb.append("Sana: ").append(date);
        }
        return sb.toString();
    }

    /**
     * UZS -> CCY hisoblash xabari. Ccy bo'sh bo'lsa USD olinadi.
     */
    public String getReverseConversionMessage(String ccyOrCode, double amountUzs) {
        String key = (ccyOrCode == null || ccyOrCode.isBlank()) ? "USD" : ccyOrCode.trim();
        try {
            List<CurrencyRate> rates = client.fetchRates();
            Optional<CurrencyRate> opt = findByCodeOrCcy(rates, key);
            if (opt.isEmpty()) {
                return "⚠️ '" + key + "' topilmadi. /list orqali mavjud kodlarni ko'ring.";
            }
            CurrencyRate r = opt.get();
            String name = preferredName(r);
            int nominal = r.getNominalAsInt();
            double rate = r.getRateAsDouble();
            String date = r.getDate();

            double perUnit = (nominal == 0) ? Double.NaN : rate / nominal; // 1 CCY = perUnit so'm

            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(new Locale("uz", "UZ"));
            df.applyPattern("#,##0.00");

            String amountUzsStr = df.format(amountUzs);
            String perUnitStr = Double.isNaN(perUnit) ? r.getRate() : df.format(perUnit);
            String foreignAmountStr = Double.isNaN(perUnit) ? "N/A" : df.format(amountUzs / perUnit);

            StringBuilder sb = new StringBuilder();
            sb.append("<b>Hisob-kitob:</b>\n");
            sb.append(amountUzsStr).append(" so'm = <b>")
                    .append(foreignAmountStr).append(" ").append(r.getCcy()).append("</b> (")
                    .append('\"').append(name).append('\"').append(")\n");
            sb.append("1 ").append(r.getCcy()).append(" = ").append(perUnitStr).append(" so'm\n");
            if (date != null && !date.isBlank()) {
                sb.append("Sana: ").append(date);
            }
            return sb.toString();
        } catch (IOException e) {
            return "⚠️ Xizmat vaqtincha mavjud emas: CBU bilan ulanishda muammo.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ So'rov bekor qilindi. Qayta urinib ko'ring.";
        } catch (Exception e) {
            return "⚠️ Kutilmagan xatolik yuz berdi. Keyinroq qayta urinib ko'ring.";
        }
    }
}
