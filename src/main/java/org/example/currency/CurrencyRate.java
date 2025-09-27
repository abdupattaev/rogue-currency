package org.example.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRate {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("Code")
    private String code; // Numeric code like "840"

    @JsonProperty("Ccy")
    private String ccy; // Alphabetic code like "USD"

    @JsonProperty("CcyNm_RU")
    private String nameRu;

    @JsonProperty("CcyNm_UZ")
    private String nameUz;

    @JsonProperty("CcyNm_UZC")
    private String nameUzc;

    @JsonProperty("CcyNm_EN")
    private String nameEn;

    @JsonProperty("Nominal")
    private String nominal; // as string in API

    @JsonProperty("Rate")
    private String rate; // as string in API

    @JsonProperty("Diff")
    private String diff; // as string in API

    @JsonProperty("Date")
    private String date; // format: dd.MM.yyyy

    // Getters
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getCcy() { return ccy; }
    public String getNameRu() { return nameRu; }
    public String getNameUz() { return nameUz; }
    public String getNameUzc() { return nameUzc; }
    public String getNameEn() { return nameEn; }
    public String getNominal() { return nominal; }
    public String getRate() { return rate; }
    public String getDiff() { return diff; }
    public String getDate() { return date; }

    // Convenience numeric accessors with safe parsing
    public double getRateAsDouble() {
        try { return Double.parseDouble(rate.replace(',', '.')); } catch (Exception e) { return Double.NaN; }
    }

    public double getDiffAsDouble() {
        try { return Double.parseDouble(diff.replace(',', '.')); } catch (Exception e) { return Double.NaN; }
    }

    public int getNominalAsInt() {
        try { return Integer.parseInt(nominal); } catch (Exception e) { return 1; }
    }
}
