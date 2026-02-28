# TwitStock

A Spring Boot web application that analyzes Twitter users' stock-picking ideas and tracks
their performance against real market data.

---

## What It Does

1. **Search a Twitter user** – Enter any Twitter/X handle.
2. **Detect stock tickers** – The app scans their recent tweets for cashtags (`$AAPL`, `$TSLA`, …)
   and plain ticker symbols, then groups them into *ideas*.
3. **Score each idea** – Using sentiment analysis and a configurable scoring model, each idea
   gets a bull/bear/neutral rating plus a confidence score.
4. **Fetch market data** – Current price, 52-week high/low, and price-change % are pulled from
   Yahoo Finance (no API key required).
5. **Browse by ticker** – See all Twitter users who have mentioned a specific stock and how
   their ideas performed.

---

## Architecture

```
twitstock/
├── pom.xml
└── src/main/
    ├── java/com/example/twitstock/
    │   ├── TwitStockApplication.java          # Spring Boot entry point
    │   ├── config/
    │   │   ├── MarketDataProperties.java      # market-data config binding
    │   │   ├── TwitStockProperties.java       # top-level config binding
    │   │   ├── TwitterScraperProperties.java  # twitter-scraper config binding
    │   │   └── WebClientConfig.java           # WebClient beans
    │   ├── controller/
    │   │   └── TwitStockController.java       # MVC endpoints
    │   ├── client/
    │   │   ├── TwitterClient.java             # interface
    │   │   ├── ExternalScraperTwitterClient.java  # calls external scraper API
    │   │   ├── MarketDataClient.java          # interface
    │   │   └── YahooFinanceClient.java        # Yahoo Finance via WebClient
    │   ├── service/
    │   │   ├── TickerDetector.java            # cashtag + symbol extraction
    │   │   ├── IdeaEngine.java                # groups tweets → ideas, scores them
    │   │   ├── AnalyticsService.java          # cross-user ticker analytics
    │   │   └── AnalysisService.java           # orchestrates the full analysis
    │   └── model/
    │       ├── Sentiment.java
    │       ├── IdeaStatus.java
    │       ├── TweetDto.java
    │       ├── Idea.java
    │       ├── UserAnalysis.java
    │       └── AnalyzeRequest.java
    └── resources/
        ├── application.yml
        ├── templates/
        │   ├── search.html          # home / search page
        │   ├── user-overview.html   # per-user idea list
        │   ├── stock-detail.html    # per-stock detail
        │   └── ticker-users.html   # all users for a ticker
        └── static/css/
            └── style.css
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17 + |
| Maven | 3.9 + |
| Twitter scraper | Any HTTP endpoint that returns tweet JSON (see below) |

---

## Configuration

All settings live in `src/main/resources/application.yml`.

```yaml
twitstock:
  twitter-scraper:
    base-url: http://localhost:3000   # URL of your Twitter scraper service
    timeout-seconds: 10
    max-tweets: 100
  market-data:
    base-url: https://query1.finance.yahoo.com
    timeout-seconds: 8
  analysis:
    max-ideas-per-user: 20
    idea-window-days: 90
```

### Twitter Scraper Contract

The app calls:

```
GET {base-url}/tweets/{username}?limit={maxTweets}
```

Expected response — a JSON array of tweet objects:

```json
[
  {
    "id": "1234567890",
    "text": "Bullish on $AAPL here, strong earnings beat",
    "created_at": "2024-01-15T14:30:00Z",
    "like_count": 42,
    "retweet_count": 12,
    "reply_count": 5
  }
]
```

Any scraper (open-source or self-hosted) that satisfies this contract will work.

---

## Running Locally

```bash
# 1. Clone
git clone https://github.com/viralharia/twitstock.git
cd twitstock

# 2. (Optional) override scraper URL
export TWITSTOCK_TWITTER_SCRAPER_BASE_URL=http://localhost:3000

# 3. Build & run
mvn spring-boot:run

# 4. Open browser
open http://localhost:8080
```

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Search page |
| `GET` | `/analyze?username={handle}` | Analyze a Twitter user |
| `GET` | `/stock/{ticker}` | Detail page for one ticker |
| `GET` | `/ticker/{ticker}/users` | All users mentioning a ticker |

---

## License

MIT
