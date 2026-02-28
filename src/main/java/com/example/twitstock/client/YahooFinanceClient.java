package com.example.twitstock.client;

import com.example.twitstock.config.TwitStockProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link MarketDataClient} implementation that queries the unofficial Yahoo
 * Finance v7 quote API (no API key required).
 *
 * <p>Endpoint called:
 * <pre>
 *   GET https://query1.finance.yahoo.com/v7/finance/quote?symbols={ticker}
 *       &fields=regularMarketPrice,regularMarketChange,
 *               regularMarketChangePercent,fiftyTwoWeekHigh,fiftyTwoWeekLow,
 *               regularMarketVolume,marketCap
 * </pre>
 *
 * <p>On error the method throws a {@link RuntimeException} so that the caller
 * (usually the controller) can handle it gracefully.
 */
@Slf4j
@Component
public class YahooFinanceClient implements MarketDataClient {

    private static final String QUOTE_PATH =
            "/v7/finance/quote?symbols={ticker}"
            + "&fields=regularMarketPrice,regularMarketChange,"
            + "regularMarketChangePercent,fiftyTwoWeekHigh,fiftyTwoWeekLow,"
            + "regularMarketVolume,marketCap";

    private final WebClient   webClient;
    private final TwitStockProperties props;
    private final ObjectMapper objectMapper;

    public YahooFinanceClient(
            @Qualifier("marketDataWebClient") WebClient webClient,
            TwitStockProperties props) {
        this.webClient    = webClient;
        this.props        = props;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ------------------------------------------------------------------ //
    //  MarketDataClient implementation                                     //
    // ------------------------------------------------------------------ //

    @Override
    public Map<String, Object> fetchQuote(String ticker) {

        int timeout = props.getMarketData().getTimeoutSeconds();
        log.debug("Fetching Yahoo Finance quote for {}", ticker);

        try {
            String json = webClient.get()
                    .uri(QUOTE_PATH, ticker)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            return parseQuote(json, ticker);

        } catch (WebClientResponseException ex) {
            log.warn("Yahoo Finance HTTP {} for {}: {}",
                    ex.getStatusCode(), ticker, ex.getResponseBodyAsString());
            throw new RuntimeException("Yahoo Finance returned " + ex.getStatusCode(), ex);

        } catch (Exception ex) {
            log.warn("Yahoo Finance call failed for {}: {}", ticker, ex.getMessage());
            throw new RuntimeException("Failed to fetch market data for " + ticker, ex);
        }
    }

    // ------------------------------------------------------------------ //
    //  Parsing                                                             //
    // ------------------------------------------------------------------ //

    private Map<String, Object> parseQuote(String json, String ticker) {
        try {
            JsonNode root   = objectMapper.readTree(json);
            JsonNode result = root.path("quoteResponse").path("result");

            if (!result.isArray() || result.isEmpty()) {
                throw new RuntimeException("No quote data found for " + ticker);
            }

            JsonNode quote = result.get(0);
            Map<String, Object> map = new LinkedHashMap<>();

            putIfPresent(map, quote, "ticker",           "symbol");
            putIfPresent(map, quote, "price",            "regularMarketPrice");
            putIfPresent(map, quote, "change",           "regularMarketChange");
            putIfPresent(map, quote, "changePercent",    "regularMarketChangePercent");
            putIfPresent(map, quote, "fiftyTwoWeekHigh", "fiftyTwoWeekHigh");
            putIfPresent(map, quote, "fiftyTwoWeekLow",  "fiftyTwoWeekLow");
            putIfPresent(map, quote, "volume",           "regularMarketVolume");
            putIfPresent(map, quote, "marketCap",        "marketCap");
            putIfPresent(map, quote, "name",             "longName");
            putIfPresent(map, quote, "exchange",         "fullExchangeName");

            return map;

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Yahoo Finance response for " + ticker, ex);
        }
    }

    private void putIfPresent(Map<String, Object> map, JsonNode node,
                              String key, String jsonField) {
        JsonNode field = node.get(jsonField);
        if (field != null && !field.isNull()) {
            if (field.isNumber())  map.put(key, field.asDouble());
            else                   map.put(key, field.asText());
        }
    }
}
