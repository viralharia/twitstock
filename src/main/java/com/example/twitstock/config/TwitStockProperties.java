package com.example.twitstock.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Top-level configuration properties bound from the {@code twitstock.*} namespace
 * in {@code application.yml}.
 *
 * <p>Nested sections:
 * <ul>
 *   <li>{@code twitstock.twitter-scraper.*} → {@link TwitterScraperProperties}</li>
 *   <li>{@code twitstock.market-data.*}     → {@link MarketDataProperties}</li>
 *   <li>{@code twitstock.analysis.*}        → inline fields below</li>
 * </ul>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "twitstock")
public class TwitStockProperties {

    private TwitterScraperProperties twitterScraper = new TwitterScraperProperties();
    private MarketDataProperties marketData = new MarketDataProperties();

    // ── analysis tunables ──────────────────────────────────────────────── //

    /** Maximum number of stock ideas to surface per user. */
    @Min(1)
    private int maxIdeasPerUser = 20;

    /**
     * Sliding window (in days) used when grouping tweets into ideas.
     * Tweets mentioning the same ticker within this window are merged.
     */
    @Min(1)
    private int ideaWindowDays = 90;
}
