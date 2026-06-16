# WeatherBridge Backend — 15-Minute Technical Presentation

## Recording Goal

This guide is designed for a **10–15 minute screen recording** covering the required deliverables:

- project structure and key design decisions;
- live demonstration of both REST endpoints;
- cache behavior and configurable TTLs;
- invalid input, upstream failure, timeout, and rate-limit handling;
- bonus temperature-alert webhook;
- setup instructions and main tradeoffs.

Recommended target duration: **14–15 minutes**.

---

## Pre-Recording Checklist

Before starting the recording, confirm:

```bash
mvn clean test
```

```bash
docker compose --env-file .env up --build -d
```

```bash
docker compose --env-file .env ps
```

```bash
curl --silent http://localhost:8080/actuator/health | jq
```

Expected health status:

```json
{
  "status": "UP"
}
```

Open these items before recording:

1. project root in the IDE;
2. `README.md`;
3. architecture diagram;
4. `WeatherController`;
5. `WeatherQueryService`;
6. `WeatherProviderPort` and `WeatherAlertPort`;
7. `OpenWeatherAdapter`;
8. `ForecastAggregationService`;
9. `TemperatureAlertService`;
10. terminal with Docker containers running;
11. a second terminal for `curl` and Redis commands;
12. the local webhook receiver, when demonstrating the bonus feature.

Use a real OpenWeather key only through environment variables. Do not show the key on screen.

---

# Presentation Timeline

## 0:00–0:45 — Introduction

### Show

- repository root;
- project name;
- short README introduction.

### Say

> This is WeatherBridge, a lightweight internal weather integration service built with Java 25 and Spring Boot 4. It consumes OpenWeather, normalizes provider-specific data, derives business-friendly fields, caches results in Redis, and exposes stable REST contracts for downstream services.
>
> The required features are two endpoints, data transformation, caching, and resilient error handling. I also implemented the optional temperature-alert webhook.

### Highlight

- Java 25;
- Spring Boot 4.0.5;
- Spring MVC and `RestClient`;
- Redis;
- Docker Compose;
- Actuator;
- hexagonal architecture.

---

## 0:45–2:15 — Project Structure

### Show

Expand the main packages:

```text
com.weatherbridge
├── adapter
│   ├── in.web
│   └── out
│       ├── openweather
│       └── webhook
├── application
│   ├── model
│   ├── port
│   │   ├── in
│   │   └── out
│   └── service
├── domain
│   ├── model
│   └── service
└── infrastructure
    ├── config
    ├── properties
    └── web
```

### Say

> The project follows a lightweight hexagonal architecture in a single Maven module.
>
> The domain contains weather models and deterministic rules. The application layer contains use cases, ports, and orchestration. Input adapters expose HTTP endpoints. Output adapters integrate with OpenWeather, Redis caching, and the webhook target.
>
> I intentionally kept one module to preserve clear boundaries without overengineering a small challenge.

### Mention representative classes

- `WeatherController` — HTTP input adapter;
- `GetCurrentWeatherUseCase` and `GetFiveDayWeatherUseCase` — input ports;
- `WeatherQueryService` — application orchestration;
- `WeatherProviderPort` — OpenWeather abstraction;
- `WeatherAlertPort` — webhook abstraction;
- `OpenWeatherAdapter` — provider output adapter;
- `HttpWeatherAlertAdapter` — webhook output adapter;
- `ForecastAggregationService` — domain transformation;
- `TemperatureThresholdPolicy` — domain alert rule.

---

## 2:15–3:45 — Architecture and Key Decisions

### Show

Display:

```text
docs/architecture/weatherbridge_hexagonal_architecture.png
```

### Say

> Dependencies point inward. Controllers depend on input ports, application services depend on output ports, and adapters implement those ports.
>
> OpenWeather DTOs stay inside the OpenWeather adapter. They are mapped into provider-independent domain models before reaching the application.
>
> Redis caching is applied at adapter boundaries. The weather caches avoid redundant provider calls, while the alert cache deduplicates successful webhook deliveries.

### Explain the main flow

```text
HTTP request
    ↓
WeatherController
    ↓
Input use-case port
    ↓
WeatherQueryService
    ↓
WeatherProviderPort
    ↓
OpenWeatherAdapter
    ↓
OpenWeather API
```

### Explain the bonus flow

```text
CurrentWeather
    ↓
TemperatureAlertService
    ↓
TemperatureThresholdPolicy
    ↓
WeatherAlertPort
    ↓
HttpWeatherAlertAdapter
    ↓
External webhook consumer
```

### Key decisions

- Spring MVC instead of WebFlux because the challenge is synchronous and small;
- Spring `RestClient` for fluent synchronous HTTP integration;
- Redis instead of in-memory cache so multiple instances can share cached data;
- no database because weather data is transient;
- no automatic retry to avoid amplifying provider rate limits;
- webhook failures are best-effort and do not fail the weather endpoint.

---

## 3:45–5:15 — Current Weather Endpoint

### Show

Open:

- `WeatherController#getCurrentWeather`;
- `WeatherLocationQuery`;
- `OpenWeatherAdapter#getCurrentWeather`;
- `OpenWeatherMapper`;
- `ThermalSensationPolicy`.

### Say

> The controller accepts a city, an optional state code, and an optional country code. Brazil is the default country.
>
> `WeatherLocationQuery` normalizes the input and generates both the OpenWeather query and the Redis cache key.
>
> The adapter requests metric units, maps the raw provider response into `CurrentWeather`, converts wind speed from meters per second to kilometers per hour, and derives the thermal-sensation category.

### Live request

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

### Point out in the response

- normalized location;
- temperature in Celsius;
- feels-like temperature;
- `difference`;
- `category`, such as `WARM`, `HOT`, or `VERY_HOT`;
- humidity;
- wind in km/h;
- provider source.

### Suggested explanation

> These are stable internal fields. Downstream consumers do not need to understand the original OpenWeather response structure or perform unit conversion themselves.

---

## 5:15–7:15 — Five-Day Summary Endpoint

### Show

Open:

- `OpenWeatherAdapter#getFiveDayForecast`;
- `OpenWeatherForecastMapper`;
- `ForecastSlice`;
- `ForecastAggregationService`;
- `PrecipitationRiskPolicy`.

### Say

> OpenWeather returns forecast entries in three-hour intervals, not one object per day.
>
> The adapter maps those entries into `ForecastSlice` objects. The aggregation service applies the city timezone, groups slices by local date, limits the result to five days, and calculates daily summaries.

### Live request

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/forecast/5-days" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

### Point out

- minimum and maximum temperature;
- average temperature;
- average feels-like temperature;
- average humidity;
- maximum precipitation probability;
- derived precipitation-risk label;
- total precipitation;
- maximum wind speed;
- dominant condition.

### Explain risk classification

```text
0–29%   → LOW
30–59%  → MODERATE
60–79%  → HIGH
80–100% → VERY_HIGH
```

### Suggested explanation

> I use the maximum probability for the day rather than the average, because an average could hide a short period with a significant rain risk.

---

## 7:15–9:15 — Redis Cache and Configurable TTLs

### Show

Open:

- `@Cacheable` on current weather and forecast methods;
- Redis cache configuration;
- `WeatherCacheProperties`;
- JSON serializer configuration.

### Say

> The current-weather and forecast caches are placed at the provider-adapter boundary. This prevents redundant external calls while keeping the application and domain independent of Spring Cache.
>
> The cache uses normalized keys and independent TTLs. Values are serialized as JSON rather than using Java native serialization.

### Clear Redis

```bash
docker compose exec redis redis-cli FLUSHALL
```

### First request — cache miss

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" > /dev/null
```

### Show API logs

```bash
docker compose --env-file .env logs --tail=100 api
```

Point out the OpenWeather adapter log:

```text
Requesting current weather
```

### Inspect keys

```bash
docker compose exec redis redis-cli --scan
```

Expected pattern:

```text
weather-bridge::current-weather::macapa:ap:br
```

### Show TTL

Copy the exact key returned by Redis:

```bash
docker compose exec redis redis-cli TTL \
  "weather-bridge::current-weather::macapa:ap:br"
```

Expected initial value: close to `600` seconds.

### Second request — cache hit

Repeat the same request:

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" > /dev/null
```

### Explain

> The controller and application service still run, but the provider adapter is not executed again while the key is valid. This reduces latency, protects the provider quota, and avoids redundant requests.

### TTL configuration

```dotenv
CURRENT_WEATHER_CACHE_TTL=10m
FORECAST_CACHE_TTL=30m
WEATHER_ALERT_CACHE_TTL=30m
```

---

## 9:15–10:45 — Error Handling

### Show

Open:

- `GlobalExceptionHandler`;
- `WeatherProviderException`;
- provider status mapping in `OpenWeatherAdapter`;
- correlation-ID filter.

### Say

> Error handling distinguishes client errors from upstream failures. Responses use Spring `ProblemDetail`, include an internal code, timestamp, request path, and correlation ID.

### Demo invalid input

```bash
curl --silent \
  "http://localhost:8080/api/v1/weather/current?countryCode=BRA" | jq
```

Or omit the required city:

```bash
curl --silent \
  "http://localhost:8080/api/v1/weather/current" | jq
```

Expected:

```text
400 INVALID_REQUEST
```

### Demo unknown city

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=CityThatDoesNotExist123456" \
  --data-urlencode "countryCode=BR" | jq
```

Expected:

```text
404 CITY_NOT_FOUND
```

### Explain upstream mapping

| Upstream condition | WeatherBridge response |
|---|---|
| OpenWeather `401` | `502 WEATHER_PROVIDER_AUTHENTICATION_ERROR` |
| OpenWeather `429` | `503 WEATHER_PROVIDER_RATE_LIMITED` |
| provider timeout | `504 WEATHER_PROVIDER_TIMEOUT` |
| provider `5xx` or connection failure | `502 WEATHER_PROVIDER_UNAVAILABLE` |
| malformed payload | `502 INVALID_PROVIDER_RESPONSE` |

### Suggested explanation

> I do not expose raw provider responses or credentials. The application translates external failures into stable internal error contracts.

---

## 10:45–12:45 — Bonus: Temperature Alert Webhook

### Show

Open:

- `TemperatureAlertService`;
- `TemperatureThresholdPolicy`;
- `WeatherAlertPort`;
- `HttpWeatherAlertAdapter`;
- `NoOpWeatherAlertAdapter`;
- `TemperatureAlertWebhookPayload`.

### Say

> The optional bonus sends an HTTP POST when the current temperature is strictly greater than a configurable threshold.
>
> The application depends on `WeatherAlertPort`, not directly on HTTP. When the feature is disabled, `NoOpWeatherAlertAdapter` is selected. When enabled, `HttpWeatherAlertAdapter` sends the POST.
>
> Successful deliveries are cached in `temperature-alert`, preventing repeated notifications for the same location and threshold during the configured TTL.

### Start the receiver

In another terminal:

```bash
python3 webhook-receiver.py
```

### Test configuration

```dotenv
WEATHER_ALERT_ENABLED=true
WEATHER_ALERT_THRESHOLD_CELSIUS=20
WEATHER_ALERT_WEBHOOK_URL=http://host.docker.internal:9090/alerts
```

### Clear Redis

```bash
docker compose exec redis redis-cli FLUSHALL
```

### Trigger the current-weather request

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

### Show the received webhook

Point out:

- event name;
- city and coordinates;
- current temperature;
- feels-like temperature;
- configured threshold;
- observation time;
- propagated correlation ID.

### Demonstrate deduplication

Repeat the same request.

Explain:

> The second request does not generate another POST while the deduplication key remains cached.

Inspect the alert key:

```bash
docker compose exec redis redis-cli --scan \
  --pattern "*temperature-alert*"
```

Expected pattern:

```text
weather-bridge::temperature-alert::macapa:ap:br:threshold:20
```

### Explain best-effort behavior

> If the webhook is unavailable, the failure is logged, but the weather endpoint still returns `200`. Failed deliveries are not cached, so a later request can try again.

---

## 12:45–14:15 — Tradeoffs and Testing Strategy

### Show

README sections:

- `Testing Strategy`;
- `Tradeoffs`;
- `Possible Improvements`.

### Main tradeoffs to state

> Redis adds infrastructure, but supports shared caching and alert deduplication across instances.
>
> Spring MVC and synchronous webhook delivery keep the implementation simple, but the request thread waits during external calls.
>
> The webhook is best-effort rather than guaranteed delivery. For a larger production system, I would publish an event to a durable queue and process delivery asynchronously.
>
> I avoided automatic retries because retries can amplify rate-limit or outage problems. A selective retry policy with backoff and a circuit breaker would be a future improvement.
>
> I kept the solution in one Maven module to avoid unnecessary structural complexity while preserving architectural boundaries.

### Testing strategy

Mention:

- unit tests for transformation policies;
- aggregation tests, including timezone boundaries;
- application-service tests with mocked ports;
- controller and `ProblemDetail` tests;
- OpenWeather adapter tests with mocked HTTP responses;
- webhook tests for success, timeout, non-2xx response, deduplication, and disabled mode;
- Testcontainers and WireMock as next improvements.

### Be transparent

If automated tests are not yet fully implemented, say:

> The current repository compiles and the flows are validated manually. The next production-hardening step would be expanding the automated test suite with Testcontainers and WireMock.

Do not claim test coverage that is not present.

---

## 14:15–15:00 — Closing

### Show

- architecture diagram;
- successful endpoint output;
- Redis keys;
- webhook payload;
- README setup section.

### Say

> WeatherBridge meets the required scope with two working REST endpoints, provider-response normalization, derived business fields, Redis caching with configurable TTLs, and structured handling for invalid input, upstream failures, timeouts, and rate limits.
>
> The bonus webhook adds configurable temperature alerts, correlation propagation, Redis deduplication, conditional activation, and best-effort failure isolation.
>
> The main architectural goal was to keep external technologies replaceable while maintaining a small and understandable codebase.

---

# Command Cheat Sheet

## Start the application

```bash
docker compose --env-file .env up --build -d
```

## Follow logs

```bash
docker compose --env-file .env logs -f api
```

## Health check

```bash
curl --silent http://localhost:8080/actuator/health | jq
```

## Current weather

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

## Five-day summary

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/forecast/5-days" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

## Clear Redis

```bash
docker compose exec redis redis-cli FLUSHALL
```

## List cache keys

```bash
docker compose exec redis redis-cli --scan
```

## Inspect a TTL

```bash
docker compose exec redis redis-cli TTL "EXACT_KEY_FROM_SCAN"
```

## Missing required city

```bash
curl --silent \
  "http://localhost:8080/api/v1/weather/current" | jq
```

## Unknown city

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=CityThatDoesNotExist123456" \
  --data-urlencode "countryCode=BR" | jq
```

## Start webhook receiver

```bash
python3 webhook-receiver.py
```

## Inspect alert cache

```bash
docker compose exec redis redis-cli --scan \
  --pattern "*temperature-alert*"
```

---

# Recording Safety and Quality Notes

- Do not expose `OPENWEATHER_API_KEY`.
- Do not expose webhook URLs containing tokens or credentials.
- Increase terminal font size before recording.
- Keep the IDE project tree expanded only where necessary.
- Clear the terminal before each live demonstration.
- Use `jq` to keep JSON readable.
- Run the complete demo once before recording.
- Keep a prepared successful response available in case OpenWeather is temporarily unavailable.
- Avoid spending too much time scrolling through code; focus on boundaries and decisions.
- Prefer explaining one representative class per architectural layer.

---

# Optional Shorter Version

If time reaches 13 minutes, shorten these parts:

- skip detailed Dockerfile explanation;
- show only one invalid-input case;
- explain rate-limit mapping without forcing a live `429`;
- summarize the testing strategy in 20 seconds;
- close immediately after the webhook deduplication demo.

The required content remains covered:

- project structure and design decisions;
- both endpoints working;
- caching behavior;
- error handling;
- bonus webhook;
- setup and tradeoffs.
