package com.example.twitstock.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated analysis result for a single Twitter user.
 *
 * <p>Produced by {@link com.example.twitstock.service.AnalysisService} and
 * stored in {@link com.example.twitstock.service.AnalyticsService} for
 * cross-user views.
 */
@Data
@Builder
public class UserAnalysis {

    // ── Identity ────────────────────────────────────────────────────────── //

    /** Twitter handle (without leading {@code @}). */
    private final String username;

    // ── Results ─────────────────────────────────────────────────────────── //

    /**
     * Ranked list of stock ideas inferred from the user's recent tweets.
     * Best ideas (highest score) appear first.
     */
    private final List<Idea> ideas;

    // ── Metadata ────────────────────────────────────────────────────────── //

    /** Total number of tweets fetched (including those with no ticker mentions). */
    private final int tweetCount;

    /** UTC timestamp when this analysis was produced. */
    private final Instant analysedAt;

    // ── Derived helpers ─────────────────────────────────────────────────── //

    /**
     * Returns the number of distinct ticker symbols mentioned across all ideas.
     */
    public int distinctTickerCount() {
        return (int) ideas.stream()
                .map(Idea::getTicker)
                .distinct()
                .count();
    }

    /**
     * Returns the number of ideas with a BULLISH dominant sentiment.
     */
    public long bullishIdeaCount() {
        return ideas.stream()
                .filter(i -> i.getSentiment() == Sentiment.BULLISH)
                .count();
    }

    /**
     * Returns the number of ideas with a BEARISH dominant sentiment.
     */
    public long bearishIdeaCount() {
        return ideas.stream()
                .filter(i -> i.getSentiment() == Sentiment.BEARISH)
                .count();
    }
}
