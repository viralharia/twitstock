package com.example.twitstock.client;

import com.example.twitstock.config.TwitStockProperties;
import com.example.twitstock.model.TweetDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * {@link TwitterClient} implementation that delegates to an external HTTP
 * scraper service.
 *
 * <p>Expected contract of the scraper:
 * <pre>
 *   GET {baseUrl}/tweets/{username}?limit={maxTweets}
 *   → 200 OK  application/json
 *   → JSON array of tweet objects (see {@link TweetDto})
 * </pre>
 *
 * <p>On any HTTP or timeout error the method logs a warning and returns an
 * empty list so the rest of the pipeline can continue gracefully.
 */
@Slf4j
@Component
public class ExternalScraperTwitterClient implements TwitterClient {

    private final WebClient            webClient;
    private final TwitStockProperties  props;
    private final ObjectMapper         objectMapper;

    public ExternalScraperTwitterClient(
            @Qualifier("twitterWebClient") WebClient webClient,
            TwitStockProperties props) {
        this.webClient    = webClient;
        this.props        = props;
        this.objectMapper = buildObjectMapper();
    }

    // ------------------------------------------------------------------ //
    //  TwitterClient implementation                                        //
    // ------------------------------------------------------------------ //

    @Override
    public List<TweetDto> fetchTweets(String username) {

        String path    = props.getTwitterScraper().tweetsPath(username);
        int    timeout = props.getTwitterScraper().getTimeoutSeconds();

        log.debug("Fetching tweets: GET {}{}", props.getTwitterScraper().getBaseUrl(), path);

        try {
            String json = webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            if (json == null || json.isBlank()) {
                log.warn("Empty response from scraper for @{}", username);
                return Collections.emptyList();
            }

            List<TweetDto> tweets = objectMapper.readValue(
                    json, new TypeReference<List<TweetDto>>() {});

            log.info("Fetched {} tweets for @{}", tweets.size(), username);
            return tweets;

        } catch (WebClientResponseException ex) {
            log.warn("HTTP {} from scraper for @{}: {}",
                    ex.getStatusCode(), username, ex.getResponseBodyAsString());
            return Collections.emptyList();

        } catch (Exception ex) {
            log.warn("Failed to fetch tweets for @{}: {}", username, ex.getMessage());
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    private static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
