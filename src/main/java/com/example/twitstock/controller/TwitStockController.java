package com.example.twitstock.controller;

import com.example.twitstock.model.UserAnalysis;
import com.example.twitstock.service.AnalysisService;
import com.example.twitstock.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Primary MVC controller for TwitStock.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET /}                        – search / home page</li>
 *   <li>{@code GET /analyze?username=…}      – per-user analysis page</li>
 *   <li>{@code GET /stock/{ticker}}          – per-ticker detail page</li>
 *   <li>{@code GET /ticker/{ticker}/users}   – all users for a ticker</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TwitStockController {

    private final AnalysisService  analysisService;
    private final AnalyticsService analyticsService;

    // ------------------------------------------------------------------ //
    //  Home / search                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Renders the search / landing page.
     */
    @GetMapping("/")
    public String home() {
        return "search";
    }

    // ------------------------------------------------------------------ //
    //  Per-user analysis                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Fetches and analyses a Twitter user's recent tweets, then renders the
     * user-overview page.
     *
     * @param username Twitter handle (with or without leading {@code @})
     * @param model    Thymeleaf model
     * @return view name
     */
    @GetMapping("/analyze")
    public String analyze(@RequestParam String username, Model model) {

        // Strip leading @ if present
        String handle = username.startsWith("@") ? username.substring(1) : username;

        log.info("Analysing Twitter user: {}", handle);

        try {
            UserAnalysis analysis = analysisService.analyse(handle);
            model.addAttribute("analysis", analysis);
            model.addAttribute("username", handle);
            return "user-overview";

        } catch (Exception ex) {
            log.error("Analysis failed for @{}: {}", handle, ex.getMessage(), ex);
            model.addAttribute("username", handle);
            model.addAttribute("error", "Could not analyse @" + handle + ": " + ex.getMessage());
            return "user-overview";
        }
    }

    // ------------------------------------------------------------------ //
    //  Per-ticker detail                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Shows detailed information for a specific stock ticker: market data
     * and all ideas involving that ticker across all analysed users.
     *
     * @param ticker uppercase stock symbol, e.g. {@code AAPL}
     * @param model  Thymeleaf model
     * @return view name
     */
    @GetMapping("/stock/{ticker}")
    public String stockDetail(@PathVariable String ticker, Model model) {

        String symbol = ticker.toUpperCase();
        log.info("Stock detail requested for: {}", symbol);

        try {
            var marketData = analyticsService.getMarketData(symbol);
            var ideas      = analyticsService.getIdeasForTicker(symbol);

            model.addAttribute("ticker",     symbol);
            model.addAttribute("marketData", marketData);
            model.addAttribute("ideas",      ideas);
            return "stock-detail";

        } catch (Exception ex) {
            log.error("Stock detail failed for {}: {}", symbol, ex.getMessage(), ex);
            model.addAttribute("ticker", symbol);
            model.addAttribute("error",  "Could not load data for " + symbol + ": " + ex.getMessage());
            return "stock-detail";
        }
    }

    // ------------------------------------------------------------------ //
    //  All users for a ticker                                              //
    // ------------------------------------------------------------------ //

    /**
     * Lists all Twitter users who have mentioned a given ticker, along with a
     * brief summary of their ideas for that stock.
     *
     * @param ticker uppercase stock symbol, e.g. {@code TSLA}
     * @param model  Thymeleaf model
     * @return view name
     */
    @GetMapping("/ticker/{ticker}/users")
    public String tickerUsers(@PathVariable String ticker, Model model) {

        String symbol = ticker.toUpperCase();
        log.info("Listing users for ticker: {}", symbol);

        try {
            List<UserAnalysis> allAnalyses = analyticsService.getAllAnalyses();

            // Filter to users who have at least one idea for this ticker,
            // then sort by idea count desc.
            List<Map<String, Object>> userRows = allAnalyses.stream()
                    .filter(ua -> ua.getIdeas().stream()
                            .anyMatch(idea -> idea.getTicker().equalsIgnoreCase(symbol)))
                    .sorted(Comparator.comparingLong(
                            (UserAnalysis ua) -> ua.getIdeas().stream()
                                    .filter(idea -> idea.getTicker().equalsIgnoreCase(symbol))
                                    .count()).reversed())
                    .map(ua -> {
                        long count = ua.getIdeas().stream()
                                .filter(idea -> idea.getTicker().equalsIgnoreCase(symbol))
                                .count();
                        return Map.<String, Object>of(
                                "username",   ua.getUsername(),
                                "ideaCount",  count
                        );
                    })
                    .collect(Collectors.toList());

            model.addAttribute("ticker",   symbol);
            model.addAttribute("userRows", userRows);
            return "ticker-users";

        } catch (Exception ex) {
            log.error("Ticker-users failed for {}: {}", symbol, ex.getMessage(), ex);
            model.addAttribute("ticker", symbol);
            model.addAttribute("error",  "Could not load users for " + symbol + ": " + ex.getMessage());
            return "ticker-users";
        }
    }
}
