package org.example.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight HTTP client for CBU currency rates API.
 */
public class CbuClient {
    private static final Logger LOGGER = Logger.getLogger(CbuClient.class.getName());
    private static final String DEFAULT_ENDPOINT = "https://cbu.uz/ru/arkhiv-kursov-valyut/json/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public CbuClient() {
        this(DEFAULT_ENDPOINT);
    }

    public CbuClient(String endpoint) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.endpoint = endpoint;
    }

    public List<CurrencyRate> fetchRates() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .build();
        LOGGER.info(() -> "CBU so'rov yuborildi: " + endpoint);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        LOGGER.info(() -> "CBU javob statusi: " + status + ", hajmi: " + (response.body() != null ? response.body().length() : 0) + " bayt");
        if (status < 200 || status >= 300) {
            throw new IOException("CBU API status kodi: " + status);
        }
        String body = response.body();
        try {
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "CBU javobini o'qishda xatolik", e);
            throw e;
        }
    }
}
