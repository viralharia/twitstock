package com.example.twitstock.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a single stock-picking idea inferred from one or more tweets.
 *
 * <p>An idea is scoped to a (username, ticker, time-window) triple. Multiple
 * tweets about the same ticker within {@code ideaWindowDays} are collapsed into
 * one idea by {@link com.example.twitstock.service.IdeaEngine}.
 */
@Data
@Builder
public class Idea {

    // ── Identity ────────────────────────────────────────────────────────── //

    /** Twitter handle of the user who posted the idea. */
    private final String username;

    /** Uppercase stock ticker symbol, e.g. {@code AAPL}. */
    private final String ticker;

    // ── Sentiment & score ───────────────────────────────────────────────── //

    /** Dominant sentiment across all tweets in this idea. */
    private final Sentiment sentiment;

    /**
     * Composite score in [−1, +1].
     * Positive = net bullish signal; negative = net bearish signal.
     */
    private final double score;

    // ── Engagement ──────────────────────────────────────────────────────── //

    /** Number of tweets that make up this idea. */
    private final int tweetCount;

    /** Total likes across all contributing tweets. */
    private final long totalLikes;

    /** Total retweets across all contributing tweets. */
    private final long totalRetweets;

    // ── Timestamps ──────────────────────────────────────────────────────── //

    /** UTC timestamp of the earliest tweet in this idea. */
    private final Instant firstSeen;

    /** UTC timestamp of the most recent tweet in this idea. */
    private final Instant lastSeen;

    // ── Status ──────────────────────────────────────────────────────────── //

    /**
     * Lifecycle status of the idea.
     * Defaults to {@link IdeaStatus#OPEN} at creation time.
     */
    private IdeaStatus status;
}
