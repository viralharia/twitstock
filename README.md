# TwitStock

Analyze Twitter (X) users' stock ideas and their historical performance.

Enter a Twitter username and a date range — TwitStock fetches the user's tweets,
extracts stock tickers mentioned in them, then looks up the historical price for
each stock at tweet time and compares it to the current price to show profit/loss.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, WebFlux (WebClient) |
| Frontend | Thymeleaf, Bootstrap 5 |
| Twitter data | [twitterapi.io](https://twitterapi.io) Advanced Search |
| Stock data | [Twelve Data](https://twelvedata.com) |

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- A **twitterapi.io API key** — sign up at <https://twitterapi.io>
- A **Twelve Data API key** — sign up at <https://twelvedata.com>

---

## Quick Start

### 1. Clone the repo

```bash
git clone https://github.com/viralharia/twitstock.git
cd twitstock
```

### 2. Set your API keys

You can provide the keys via environment variables (recommended) or by editing
`src/main/resources/application.yml` directly.

**Environment variables (recommended)**

```bash
export TWITTER_SCRAPER_API_KEY="your_twitterapi_io_key"
export TWELVEDATA_API_KEY="your_twelve_data_key"
```

**Or edit `application.yml`**

```yaml
twitter:
  scraper:
    api-key: "your_twitterapi_io_key"

twelvedata:
  api:
    key: "your_twelve_data_key"
```

### 3. Build & run

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080> in your browser.

---

## Configuration Reference

All settings live under `src/main/resources/application.yml`.

| Property | Env var | Default | Description |
|---|---|---|---|
| `twitter.scraper.api-key` | `TWITTER_SCRAPER_API_KEY` | _(empty)_ | Your twitterapi.io API key |
| `twitter.scraper.base-url` | — | `https://api.twitterapi.io` | Base URL for twitterapi.io |
| `twitter.scraper.max-pages` | — | `5` | Max pagination pages per fetch (≈100 tweets) |
| `twelvedata.api.key` | `TWELVEDATA_API_KEY` | _(empty)_ | Your Twelve Data API key |

---

## How It Works

1. **Tweet fetch** — `ExternalScraperTwitterClient` calls the twitterapi.io
   `GET /twitter/tweet/advanced_search` endpoint with a query like:
   ```
   from:elonmusk since:2024-01-01 until:2024-04-01 -is:reply -is:retweet
   ```
   Server-side operators handle date filtering and reply/retweet exclusion.
   Cursor-based pagination collects up to `max-pages × 20` tweets.

2. **Ticker extraction** — A regex scans each tweet's text for `$TICKER` patterns
   (e.g. `$AAPL`, `$TSLA`).

3. **Price lookup** — For each (ticker, tweet-date) pair, Twelve Data's
   `/time_series` endpoint returns the closing price on that date.

4. **P&L calculation** — The app compares the tweet-date price to today's price
   and shows the gain/loss percentage.

---

## Project Structure

```
src/main/java/com/example/twitstock/
├── client/
│   ├── TwitterClient.java                 # Interface
│   └── ExternalScraperTwitterClient.java  # twitterapi.io implementation
├── config/
│   └── TwitterScraperProperties.java      # Bound from application.yml
├── model/
│   └── TweetDto.java                      # Tweet data transfer object
├── service/
│   └── TweetAnalysisService.java          # Orchestrates fetch → extract → price
└── controller/
    └── AnalysisController.java            # Spring MVC controller
```

---

## License

MIT
