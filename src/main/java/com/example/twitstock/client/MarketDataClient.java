package com.example.twitstock.client;

import java.util.Map;

/**
 * Abstraction over the market-data source.
 *
 * <p>The default implementation ({@link YahooFinanceClient}) fetches data from
 * the unofficial Yahoo Finance query API.  Swap for a different implementation
 * (Bloomberg, Polygon, etc.) by changing the Spring bean.
 */
public interface MarketDataClient {

    /**
     * Fetches a real-time (or near-real-time) quote for the given ticker.
     *
     * @param ticker uppercase stock symbol, e.g. {@code "AAPL"}
     * @return map of quote fields: at minimum {@code price}, {@code change},
     *         {@code changePercent}, {@code fiftyTwoWeekHigh},
     *         {@code fiftyTwoWeekLow}, {@code volume}, {@code marketCap}
     * @throws RuntimeException if the upstream call fails
     */
    Map<String, Object> fetchQuote(String ticker);
}
