package com.example.twitstock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for the twitterapi.io client.
 *
 * <p>Bound from the {@code twitter.scraper} prefix in {@code application.yml}.</p>
 *
 * <h3>Required properties</h3>
 * <ul>
 *   <li>{@code twitter.scraper.api-key} — your twitterapi.io API key</li>
 *   <li>{@code twitter.scraper.base-url} — API base URL (default: https://api.twitterapi.io)</li>
 *   <li>{@code twitter.scraper.max-pages} — max pagination pages per request (default: 5)</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "twitter.scraper")
public class TwitterScraperProperties {

    /** twitterapi.io API key — sent as the {@code X-API-Key} request header. */
    private String apiKey = "";

    /** Base URL for the twitterapi.io REST API. */
    private String baseUrl = "https://api.twitterapi.io";

    /**
     * Maximum number of pagination pages to fetch per {@code fetchTweets} call.
     * Each page returns up to 20 tweets. Raise this for longer date windows.
     */
    private int maxPages = 5;

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getApiKey()              { return apiKey; }
    public void setApiKey(String apiKey)   { this.apiKey = apiKey; }

    public String getBaseUrl()             { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getMaxPages()               { return maxPages; }
    public void setMaxPages(int maxPages)  { this.maxPages = maxPages; }
}