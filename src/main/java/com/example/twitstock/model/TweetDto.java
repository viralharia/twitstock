package com.example.twitstock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lightweight DTO that maps a single tweet from the external scraper's JSON
 * response.
 *
 * <p>Field names follow the snake_case convention used by most scraper APIs;
 * Jackson's {@code @JsonProperty} annotations handle the mapping to camelCase
 * Java fields.
 */
@Data
@NoArgsConstructor
public class TweetDto {

    /** Platform-assigned tweet ID. */
    private String id;

    /** Full text of the tweet. */
    private String text;

    /** UTC timestamp when the tweet was posted. */
    @JsonProperty("created_at")
    private Instant createdAt;

    /** Number of likes (hearts) the tweet has received. */
    @JsonProperty("like_count")
    private long likeCount;

    /** Number of retweets. */
    @JsonProperty("retweet_count")
    private long retweetCount;

    /** Number of replies. */
    @JsonProperty("reply_count")
    private long replyCount;
}
