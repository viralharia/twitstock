package com.example.twitstock.client;

import com.example.twitstock.model.TweetDto;

import java.util.List;

/**
 * Abstraction over the Twitter / X data source.
 *
 * <p>The default implementation ({@link ExternalScraperTwitterClient}) calls an
 * external scraper HTTP service.  Swap this interface for a different
 * implementation (e.g. a mock or the official API) by changing the Spring bean.
 */
public interface TwitterClient {

    /**
     * Fetches the most recent tweets for the given Twitter handle.
     *
     * @param username Twitter handle, without the leading {@code @}
     * @return list of tweet DTOs; never {@code null}, may be empty
     * @throws RuntimeException if the upstream call fails
     */
    List<TweetDto> fetchTweets(String username);
}
