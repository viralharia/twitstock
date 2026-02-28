package com.example.twitstock.service;

import com.example.twitstock.config.TwitStockProperties;
import com.example.twitstock.model.Idea;
import com.example.twitstock.model.IdeaStatus;
import com.example.twitstock.model.Sentiment;
import com.example.twitstock.model.TweetDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts a list of raw tweets into a ranked list of stock-pick {@link Idea}s.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Detect all ticker symbols in each tweet ({@link TickerDetector}).</li>
 *   <li>Group tweets by ticker.</li>
 *   <li>Collapse adjacent tweets (within {@code ideaWindowDays}) into a single
 *       idea per ticker per window.</li>
 *   <li>Score each idea (sentiment + engagement heuristics).</li>
 *   <li>Return the top-N ideas sorted by score descending.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaEngine {

    private final TickerDetector      tickerDetector;
    private final TwitStockProperties props;

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Produces a ranked list of stock ideas from {@code tweets}.
     *
     * @param username Twitter handle (used to populate {@link Idea#getUsername()})
     * @param tweets   raw tweets fetched from the scraper
     * @return ordered list of ideas, best first; never {@code null}
     */
    public List<Idea> buildIdeas(String username, List<TweetDto> tweets) {
        if (tweets == null || tweets.isEmpty()) {
            return Collections.emptyList();
        }

        // ── 1. Annotate each tweet with detected tickers ──────────────── //
        Map<TweetDto, Set<String>> tweetTickers = new LinkedHashMap<>();
        for (TweetDto tweet : tweets) {
            Set<String> tickers = tickerDetector.detect(tweet.getText());
            if (!tickers.isEmpty()) {
                tweetTickers.put(tweet, tickers);
            }
        }

        log.debug("[{}] {}/{} tweets contain ticker symbols",
                username, tweetTickers.size(), tweets.size());

        // ── 2. Group tweets by ticker ─────────────────────────────────── //
        Map<String, List<TweetDto>> byTicker = new LinkedHashMap<>();
        tweetTickers.forEach((tweet, tickers) ->
                tickers.forEach(ticker ->
                        byTicker.computeIfAbsent(ticker, k -> new ArrayList<>()).add(tweet)));

        // ── 3. Build ideas (one per ticker-window) ────────────────────── //
        List<Idea> ideas = new ArrayList<>();
        for (Map.Entry<String, List<TweetDto>> entry : byTicker.entrySet()) {
            ideas.addAll(buildIdeasForTicker(username, entry.getKey(), entry.getValue()));
        }

        // ── 4. Sort by score desc, cap at maxIdeasPerUser ─────────────── //
        ideas.sort(Comparator.comparingDouble(Idea::getScore).reversed());
        int cap = props.getMaxIdeasPerUser();
        if (ideas.size() > cap) {
            ideas = ideas.subList(0, cap);
        }

        log.info("[{}] Generated {} ideas from {} tweets", username, ideas.size(), tweets.size());
        return ideas;
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Collapses tweets for a single ticker into windowed ideas.
     *
     * <p>Tweets are sorted chronologically first; then any run of tweets whose
     * timestamps fall within {@code ideaWindowDays} of the first tweet in the
     * run are merged into one idea.
     */
    private List<Idea> buildIdeasForTicker(String username, String ticker, List<TweetDto> tweets) {

        // Sort oldest-first so the window opens at the earliest tweet
        List<TweetDto> sorted = tweets.stream()
                .filter(t -> t.getCreatedAt() != null)
                .sorted(Comparator.comparing(TweetDto::getCreatedAt))
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return Collections.emptyList();

        List<Idea> ideas    = new ArrayList<>();
        List<TweetDto> window = new ArrayList<>();
        Instant windowStart  = sorted.get(0).getCreatedAt();

        for (TweetDto tweet : sorted) {
            long daysSinceStart = ChronoUnit.DAYS.between(windowStart, tweet.getCreatedAt());
            if (daysSinceStart <= props.getIdeaWindowDays()) {
                window.add(tweet);
            } else {
                // Flush current window and start a new one
                ideas.add(scoreIdea(username, ticker, new ArrayList<>(window)));
                window.clear();
                window.add(tweet);
                windowStart = tweet.getCreatedAt();
            }
        }
        if (!window.isEmpty()) {
            ideas.add(scoreIdea(username, ticker, window));
        }

        return ideas;
    }

    /**
     * Scores a single idea derived from a list of tweets about the same ticker
     * within one time window.
     *
     * <h3>Scoring model</h3>
     * <ul>
     *   <li><b>Sentiment contribution</b> – each tweet is classified as
     *       BULLISH (+1), BEARISH (−1) or NEUTRAL (0); the net score is
     *       divided by the number of tweets to produce a value in [−1, +1].</li>
     *   <li><b>Engagement contribution</b> – log-normalised sum of likes +
     *       2×retweets + replies, capped at 0.5 to limit influence.</li>
     *   <li><b>Frequency contribution</b> – log of tweet count (≥ 1),
     *       capped at 0.3.</li>
     * </ul>
     *
     * <p>Final score = sentiment + engagement + frequency, clamped to [−1, +1].
     */
    private Idea scoreIdea(String username, String ticker, List<TweetDto> tweets) {

        // ── Sentiment ───────────────────────────────────────────────────── //
        int    sentimentSum  = 0;
        int    bullishCount  = 0;
        int    bearishCount  = 0;
        long   totalLikes    = 0;
        long   totalRetweets = 0;
        long   totalReplies  = 0;

        for (TweetDto t : tweets) {
            Sentiment s = classifySentiment(t.getText());
            switch (s) {
                case BULLISH -> { sentimentSum++; bullishCount++; }
                case BEARISH -> { sentimentSum--; bearishCount++; }
                default      -> { /* NEUTRAL */ }
            }
            totalLikes    += t.getLikeCount();
            totalRetweets += t.getRetweetCount();
            totalReplies  += t.getReplyCount();
        }

        double sentimentScore = tweets.isEmpty() ? 0.0 : (double) sentimentSum / tweets.size();

        // ── Engagement ──────────────────────────────────────────────────── //
        long   totalEngagement = totalLikes + 2L * totalRetweets + totalReplies;
        double engagementScore = Math.min(0.5, Math.log1p(totalEngagement) / 20.0);

        // ── Frequency ───────────────────────────────────────────────────── //
        double frequencyScore  = Math.min(0.3, Math.log1p(tweets.size()) / 10.0);

        // ── Combined & clamped ──────────────────────────────────────────── //
        double raw   = sentimentScore + engagementScore + frequencyScore;
        double score = Math.max(-1.0, Math.min(1.0, raw));

        // ── Dominant sentiment ──────────────────────────────────────────── //
        Sentiment dominant;
        if      (bullishCount > bearishCount)  dominant = Sentiment.BULLISH;
        else if (bearishCount > bullishCount)  dominant = Sentiment.BEARISH;
        else                                   dominant = Sentiment.NEUTRAL;

        // ── Timestamps ──────────────────────────────────────────────────── //
        Instant firstSeen = tweets.stream()
                .map(TweetDto::getCreatedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Instant lastSeen = tweets.stream()
                .map(TweetDto::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return Idea.builder()
                .username(username)
                .ticker(ticker)
                .sentiment(dominant)
                .score(score)
                .tweetCount(tweets.size())
                .totalLikes(totalLikes)
                .totalRetweets(totalRetweets)
                .firstSeen(firstSeen)
                .lastSeen(lastSeen)
                .status(IdeaStatus.OPEN)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Sentiment classifier                                                //
    // ------------------------------------------------------------------ //

    /** Bullish signal words / phrases. */
    private static final List<String> BULLISH_SIGNALS = List.of(
            "buy", "long", "bull", "bullish", "upside", "breakout", "accumulate",
            "strong buy", "outperform", "upgrade", "buying", "bought", "calls",
            "moon", "rocket", "squeeze", "undervalued", "cheap", "dip",
            "oversold", "support", "bounce", "reversal", "rally", "rip",
            "beat", "beats", "blowout", "record", "surprise"
    );

    /** Bearish signal words / phrases. */
    private static final List<String> BEARISH_SIGNALS = List.of(
            "sell", "short", "bear", "bearish", "downside", "breakdown",
            "strong sell", "underperform", "downgrade", "selling", "sold", "puts",
            "overvalued", "expensive", "overbought", "resistance", "rollover",
            "miss", "misses", "disappoint", "warning", "cut", "lower",
            "dump", "crash", "tank", "collapse", "fail"
    );

    /**
     * Classifies a single tweet text as BULLISH, BEARISH, or NEUTRAL.
     *
     * <p>Strategy: count keyword hits for each polarity; whichever has more
     * hits wins.  Ties → NEUTRAL.
     *
     * @param text raw tweet text
     * @return sentiment classification
     */
    Sentiment classifySentiment(String text) {
        if (text == null || text.isBlank()) return Sentiment.NEUTRAL;
        String lower = text.toLowerCase();

        long bullHits = BULLISH_SIGNALS.stream().filter(lower::contains).count();
        long bearHits = BEARISH_SIGNALS.stream().filter(lower::contains).count();

        if      (bullHits > bearHits) return Sentiment.BULLISH;
        else if (bearHits > bullHits) return Sentiment.BEARISH;
        else                          return Sentiment.NEUTRAL;
    }
}
