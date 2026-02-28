package com.example.twitstock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Produces the {@link WebClient} instances used by the HTTP client layer.
 *
 * <p>Two named beans are registered:
 * <ul>
 *   <li>{@code twitterWebClient}    – points at the external Twitter scraper</li>
 *   <li>{@code marketDataWebClient} – points at the market-data backend (Yahoo Finance)</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    @Bean("twitterWebClient")
    public WebClient twitterWebClient(TwitStockProperties props) {
        TwitterScraperProperties scraper = props.getTwitterScraper();
        return WebClient.builder()
                .baseUrl(scraper.getBaseUrl())
                .build();
    }

    @Bean("marketDataWebClient")
    public WebClient marketDataWebClient(TwitStockProperties props) {
        MarketDataProperties md = props.getMarketData();
        return WebClient.builder()
                .baseUrl(md.getBaseUrl())
                .build();
    }
}
