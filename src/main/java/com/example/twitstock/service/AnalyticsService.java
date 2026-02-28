package com.example.twitstock.service;

import com.example.twitstock.client.MarketDataClient;
import com.example.twitstock.model.Idea;
import com.example.twitstock.model.UserAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides cross-user analytics: per-ticker market data and aggregated idea
 * lists across all users that have been analysed in this JVM session.
 *
 * <p><b>In-memory store:</b> {@code AnalysisService} registers each completed
 * {@link UserAnalysis} here so that the ticker and multi-user views have data
 * to display without requiring a persistent database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MarketDataClient marketDataClient;

    /**
     * Session-scoped store: username → latest {@link UserAnalysis}.
     * Thread-safe for concurrent web requests.
     */
    private final Map<String, UserAnalysis> store = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ //
    //  Registration (called by AnalysisService)                           //
    // ------------------------------------------------------------------ //

    /**
     * Stores or replaces the analysis for the given user.
     *
     * @param analysis completed analysis; must not be {@code null}
     */
    public void register(UserAnalysis analysis) {
        Objects.requireNonNull(analysis, "analysis must not be null");
        store.put(analysis.getUsername().toLowerCase(), analysis);
        log.debug("Registered analysis for @{} ({} ideas)",
                analysis.getUsername(), analysis.getIdeas().size());
    }

    // ------------------------------------------------------------------ //
    //  Query API                                                           //
    // ------------------------------------------------------------------ //

    /**
     * Returns all {@link UserAnalysis} objects stored in this session,
     * ordered by username ascending.
     */
    public List<UserAnalysis> getAllAnalyses() {
        return store.values().stream()
                .sorted(Comparator.comparing(UserAnalysis::getUsername))
                .collect(Collectors.toList());
    }

    /**
     * Returns all {@link Idea}s for a given ticker across all stored users,
     * sorted by score descending.
     *
     * @param ticker uppercase stock symbol
     * @return list of ideas; empty if none found
     */
    public List<Idea> getIdeasForTicker(String ticker) {
        String symbol = ticker.toUpperCase();
        return store.values().stream()
                .flatMap(ua -> ua.getIdeas().stream())
                .filter(idea -> idea.getTicker().equalsIgnoreCase(symbol))
                .sorted(Comparator.comparingDouble(Idea::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Fetches current market data for the given ticker.
     *
     * @param ticker uppercase stock symbol
     * @return map of market-data fields (price, change%, 52-week hi/lo, etc.)
     */
    public Map<String, Object> getMarketData(String ticker) {
        try {
            return marketDataClient.fetchQuote(ticker);
        } catch (Exception ex) {
            log.warn("Market-data fetch failed for {}: {}", ticker, ex.getMessage());
            return Map.of("error", "Market data unavailable: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Stats / summary                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Returns a map of ticker → idea count across all stored users,
     * sorted by count descending.  Useful for a "trending tickers" widget.
     */
    public Map<String, Long> tickerFrequency() {
        return store.values().stream()
                .flatMap(ua -> ua.getIdeas().stream())
                .collect(Collectors.groupingBy(
                        idea -> idea.getTicker().toUpperCase(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
