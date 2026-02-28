package com.example.twitstock.service;

import com.example.twitstock.client.TwitterClient;
import com.example.twitstock.model.TweetDto;
import com.example.twitstock.model.UserAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the full analysis pipeline for a single Twitter user:
 *
 * <ol>
 *   <li>Fetch recent tweets via {@link TwitterClient}.</li>
 *   <li>Build stock ideas via {@link IdeaEngine}.</li>
 *   <li>Persist the result in {@link AnalyticsService} for cross-user views.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TwitterClient    twitterClient;
    private final IdeaEngine       ideaEngine;
    private final AnalyticsService analyticsService;

    /**
     * Runs the full analysis pipeline for the given Twitter handle.
     *
     * @param username Twitter handle (without leading {@code @})
     * @return completed {@link UserAnalysis}
     * @throws RuntimeException if the tweet fetch fails
     */
    public UserAnalysis analyse(String username) {

        log.info("Starting analysis for @{}", username);

        // ── 1. Fetch tweets ──────────────────────────────────────────── //
        List<TweetDto> tweets = twitterClient.fetchTweets(username);
        log.info("Fetched {} tweets for @{}", tweets.size(), username);

        // ── 2. Build ideas ───────────────────────────────────────────── //
        var ideas = ideaEngine.buildIdeas(username, tweets);
        log.info("Built {} ideas for @{}", ideas.size(), username);

        // ── 3. Assemble result ───────────────────────────────────────── //
        UserAnalysis analysis = UserAnalysis.builder()
                .username(username)
                .ideas(ideas)
                .tweetCount(tweets.size())
                .analysedAt(Instant.now())
                .build();

        // ── 4. Register for cross-user analytics ─────────────────────── //
        analyticsService.register(analysis);

        return analysis;
    }
}
