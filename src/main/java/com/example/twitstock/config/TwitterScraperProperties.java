package com.example.twitstock.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to twitstock.twitter-scraper.* in application.yml
 */
@Data
@Validated
public class TwitterScraperProperties {

    /** Base URL of the external Twitter / X scraper service. */
    @NotBlank
    private String baseUrl = "http://localhost:3000";

    /** HTTP timeout in seconds for scraper calls. */
    @Min(1)
    private int timeoutSeconds = 10;

    /** Maximum number of tweets to fetch per user. */
    @Min(1)
    private int maxTweets = 100;

    // ------------------------------------------------------------------ //
    //  Computed helpers (not bound from config)                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns the tweets endpoint for the given username.
     * e.g. {@code /tweets/elonmusk?limit=100}
     */
    public String tweetsPath(String username) {
        return "/tweets/" + username + "?limit=" + maxTweets;
    }
}
