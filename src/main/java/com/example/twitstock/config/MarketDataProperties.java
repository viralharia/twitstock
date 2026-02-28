package com.example.twitstock.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to twitstock.market-data.* in application.yml
 */
@Data
@Validated
public class MarketDataProperties {

    /** Base URL of the market-data backend (default: Yahoo Finance query API). */
    @NotBlank
    private String baseUrl = "https://query1.finance.yahoo.com";

    /** HTTP timeout in seconds for market-data calls. */
    @Min(1)
    private int timeoutSeconds = 8;
}
