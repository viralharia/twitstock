package com.example.twitstock.client;

import com.example.twitstock.config.TwitterScraperProperties;
import com.example.twitstock.model.TweetDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TwitterClient implementation using <a href="https://twitterapi.io">twitterapi.io</a>.
 *
 * <p>Uses the <b>Advanced Search</b> endpoint which supports Twitter search operators
 * for date-range filtering, user scoping, and reply/retweet exclusion — all server-side.</p>
 *
 * <p>Endpoint: {@code GET https://api.twitterapi.io/twitter/tweet/advanced_search}</p>
 * <p>Auth: {@code X-API-Key} header.</p>
 * <p>Pagination: cursor-based ({@code next_cursor} / {@code has_next_page}).</p>
 *
 * @see <a href="https://docs.twitterapi.io/api-reference/endpoint/tweet_advanced_search">API Docs</a>
 */
@Component
public class ExternalScraperTwitterClient implements TwitterClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalScraperTwitterClient.class);

    /**
     * Twitter's classic timestamp format used in the {@code createdAt} response field.
     * Example: "Tue Dec 10 07:00:30 +0000 2024"
     */
    private static final DateTimeFormatter TWITTER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    private final WebClient webClient;
    private final TwitterScraperProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExternalScraperTwitterClient(WebClient.Builder webClientBuilder,
                                        TwitterScraperProperties props) {
        this.props = props;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", props.getApiKey())
                .build();
    }

    // -------------------------------------------------------------------------
    // TwitterClient contract
    // -------------------------------------------------------------------------

    @Override
    public List<TweetDto> fetchTweets(String username, LocalDate startDate, LocalDate endDate) {
        String query = buildQuery(username, startDate, endDate);
        log.info("twitterapi.io advanced_search | query='{}'", query);

        List<TweetDto> all = new ArrayList<>();
        String cursor = null;
        int page = 0;

        do {
            JsonNode response = fetchPage(query, cursor);
            if (response == null) break;

            JsonNode tweets = response.path("tweets");
            if (!tweets.isArray() || tweets.isEmpty()) {
                log.debug("No tweets on page {}", page);
                break;
            }

            for (JsonNode tweet : tweets) {
                parseTweet(tweet).ifPresent(all::add);
            }

            boolean hasNext = response.path("has_next_page").asBoolean(false);
            cursor = hasNext ? response.path("next_cursor").asText(null) : null;
            page++;

            log.debug("Page {} fetched {} tweets; hasNext={}", page, tweets.size(), hasNext);

        } while (cursor != null && page < props.getMaxPages());

        log.info("twitterapi.io returned {} raw tweets for @{}", all.size(), username);
        return all;
    }

    // -------------------------------------------------------------------------
    // Query builder
    // -------------------------------------------------------------------------

    /**
     * Builds a Twitter advanced-search query string.
     *
     * <p>Example output for username="elonmusk", startDate=2024-01-01, endDate=2024-03-31:</p>
     * <pre>from:elonmusk since:2024-01-01 until:2024-04-01 -is:reply -is:retweet</pre>
     *
     * <p>The {@code until:} date is <em>exclusive</em> in Twitter's operators, so we add 1 day
     * to make the range end-inclusive for the caller.</p>
     */
    private String buildQuery(String username, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("from:").append(username);

        if (startDate != null) {
            sb.append(" since:").append(startDate);
        }
        if (endDate != null) {
            // Twitter's until: is exclusive — add one day to include endDate itself
            sb.append(" until:").append(endDate.plusDays(1));
        }

        sb.append(" -is:reply -is:retweet");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // HTTP layer
    // -------------------------------------------------------------------------

    private JsonNode fetchPage(String query, String cursor) {
        try {
            WebClient.RequestHeadersSpec<?> req = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder
                                .path("/twitter/tweet/advanced_search")
                                .queryParam("query", query)
                                .queryParam("queryType", "Latest");
                        if (cursor != null) {
                            uriBuilder.queryParam("cursor", cursor);
                        }
                        return uriBuilder.build();
                    });

            String body = req.retrieve().bodyToMono(String.class).block();
            return objectMapper.readTree(body);

        } catch (WebClientResponseException e) {
            log.error("twitterapi.io HTTP {} {}: {}",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("twitterapi.io fetch error: {}", e.getMessage(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Tweet parsing
    // -------------------------------------------------------------------------

    /**
     * Maps a single tweet JSON node to a {@link TweetDto}.
     *
     * <p>Returns {@link Optional#empty()} when the tweet cannot be parsed or is outside
     * the requested date window (belt-and-suspenders guard, since the API already filters).</p>
     */
    private Optional<TweetDto> parseTweet(JsonNode tweet) {
        try {
            String id   = tweet.path("id").asText();
            String text = tweet.path("text").asText("");
            String rawDate = tweet.path("createdAt").asText("");

            ZonedDateTime createdAt = parseDate(rawDate);
            if (createdAt == null) {
                log.warn("Skipping tweet {} — unparseable date '{}'", id, rawDate);
                return Optional.empty();
            }

            // Metadata flags — used upstream to double-check reply/RT exclusion
            boolean isReply   = tweet.path("isReply").asBoolean(false);
            boolean isRetweet = tweet.path("hasRetweetedTweet").asBoolean(false)
                             || tweet.path("hasQuotedTweet").asBoolean(false);

            // Author
            String author = tweet.path("author")
                                 .path("userName").asText("");

            // Engagement
            int likes    = tweet.path("likeCount").asInt(0);
            int retweets = tweet.path("retweetCount").asInt(0);
            int replies  = tweet.path("replyCount").asInt(0);

            TweetDto dto = new TweetDto();
            dto.setId(id);
            dto.setText(text);
            dto.setCreatedAt(createdAt);
            dto.setAuthor(author);
            dto.setLikes(likes);
            dto.setRetweets(retweets);
            dto.setReplies(replies);
            dto.setIsReply(isReply);
            dto.setIsRetweet(isRetweet);

            return Optional.of(dto);

        } catch (Exception e) {
            log.warn("Failed to parse tweet node: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ZonedDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ZonedDateTime.parse(raw, TWITTER_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            log.debug("Date parse failed for '{}': {}", raw, e.getMessage());
            return null;
        }
    }
}