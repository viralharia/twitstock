package com.example.twitstock.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects stock-ticker symbols mentioned in free-form tweet text.
 *
 * <p>Detection strategies (applied in order, results de-duplicated):
 * <ol>
 *   <li><b>Cashtags</b> – any token of the form {@code $TICKER} where TICKER is
 *       1–5 uppercase letters, e.g. {@code $AAPL}, {@code $TSLA}.</li>
 *   <li><b>Plain symbols</b> – a curated allow-list of well-known tickers that
 *       can appear without a leading {@code $}.  Only exact whole-word matches
 *       are accepted to minimise false positives.</li>
 * </ol>
 *
 * <p>Symbols in the {@link #BLOCKED_WORDS} set are always excluded regardless
 * of how they were detected (prevents common English words being mis-classified).
 */
@Service
public class TickerDetector {

    // ------------------------------------------------------------------ //
    //  Cashtag pattern                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Matches {@code $TICKER} where TICKER is 1–5 uppercase ASCII letters.
     * The dollar sign must not be preceded by another dollar sign or letter
     * (avoids matching currency amounts like {@code $$} or email addresses).
     */
    private static final Pattern CASHTAG_PATTERN =
            Pattern.compile("(?<![\\w$])\\$([A-Z]{1,5})(?![\\w])");

    // ------------------------------------------------------------------ //
    //  Plain-symbol allow-list                                             //
    // ------------------------------------------------------------------ //

    /** Well-known tickers that frequently appear without a leading {@code $}. */
    private static final Set<String> KNOWN_TICKERS = Set.of(
            "AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "META", "TSLA", "NVDA",
            "AMD", "INTC", "NFLX", "BABA", "TSM", "ORCL", "IBM", "QCOM",
            "AVGO", "TXN", "MU",  "AMAT", "LRCX", "KLAC", "SNPS", "CDNS",
            "CRM",  "NOW",  "WDAY", "ADSK", "INTU", "PANW", "CRWD", "ZS",
            "OKTA", "NET",  "DDOG", "SNOW", "MDB",  "ESTC", "CFLT", "GTLB",
            "SHOP", "SQ",   "PYPL", "COIN", "HOOD", "AFRM", "UPST", "SOFI",
            "JPM",  "BAC",  "GS",   "MS",   "WFC",  "C",    "BRK",  "V",
            "MA",   "AXP",  "DIS",  "NKLA", "RIVN", "LCID",
            "F",    "GM",   "STLA", "TM",   "NIO",  "LI",   "XPEV",
            "BA",   "LMT",  "RTX",  "NOC",  "GD",   "GE",   "CAT",  "DE",
            "XOM",  "CVX",  "COP",  "SLB",  "MPC",  "VLO",  "OXY",
            "JNJ",  "PFE",  "MRNA", "BNTX", "ABBV", "LLY",  "UNH",  "CVS",
            "WMT",  "TGT",  "COST", "AMZN", "HD",   "LOW",  "NKE",  "LULU",
            "SPY",  "QQQ",  "IWM",  "GLD",  "SLV",  "TLT",  "VIX"
    );

    // ------------------------------------------------------------------ //
    //  Blocked words                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Common English words (and known non-ticker cashtags) that should never
     * be treated as stock tickers even if they appear with a leading {@code $}.
     */
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "A", "I", "AM", "AN", "AS", "AT", "BE", "BY", "DO", "GO",
            "HE", "IF", "IN", "IS", "IT", "ME", "MY", "NO", "OF", "OK",
            "ON", "OR", "SO", "TO", "UP", "US", "WE",
            "THE", "AND", "FOR", "ARE", "BUT", "NOT", "YOU", "ALL",
            "CAN", "HER", "WAS", "ONE", "OUR", "OUT", "DAY", "GET",
            "HAS", "HIM", "HIS", "HOW", "ITS", "LET", "MAY", "NEW",
            "NOW", "OLD", "OWN", "SAY", "SHE", "TOO", "USE", "WAY",
            "WHO", "WIN", "YES", "YET",
            "USD", "EUR", "GBP", "JPY", "BTC", "ETH", "USDC", "USDT"
    );

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Extracts all ticker symbols detected in {@code text}.
     *
     * @param text raw tweet text; must not be {@code null}
     * @return ordered, de-duplicated set of uppercase ticker symbols
     */
    public Set<String> detect(String text) {
        Set<String> tickers = new LinkedHashSet<>();
        tickers.addAll(extractCashtags(text));
        tickers.addAll(extractKnownSymbols(text));
        tickers.removeAll(BLOCKED_WORDS);
        return tickers;
    }

    /**
     * Convenience overload — detects tickers in multiple texts and merges
     * the results.
     *
     * @param texts collection of tweet texts
     * @return merged, de-duplicated set of uppercase ticker symbols
     */
    public Set<String> detectAll(Collection<String> texts) {
        Set<String> tickers = new LinkedHashSet<>();
        for (String text : texts) {
            tickers.addAll(detect(text));
        }
        return tickers;
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    private Set<String> extractCashtags(String text) {
        Set<String> result = new LinkedHashSet<>();
        // Uppercase the text so cashtag matching is case-insensitive
        Matcher m = CASHTAG_PATTERN.matcher(text.toUpperCase());
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    private Set<String> extractKnownSymbols(String text) {
        Set<String> result = new LinkedHashSet<>();
        String upper = text.toUpperCase();
        for (String ticker : KNOWN_TICKERS) {
            // Whole-word match only — avoid false positives like "AMZN" inside
            // "GLAMZN" or "APPLE" matching "AAPL".
            Pattern p = Pattern.compile("(?<![A-Z])" + Pattern.quote(ticker) + "(?![A-Z])");
            if (p.matcher(upper).find()) {
                result.add(ticker);
            }
        }
        return result;
    }
}
