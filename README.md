## Main Features

* Current weather lookup by city
* Five-day weather summary
* OpenWeather integration
* Metric unit normalization
* Wind-speed conversion from meters per second to kilometers per hour
* Thermal-sensation classification
* Precipitation-risk classification
* Three-hour forecast aggregation into daily summaries
* Timezone-aware forecast grouping
* Redis-backed caching
* Independent configurable cache TTLs
* Standardized API errors using `ProblemDetail`
* Provider authentication-error handling
* Provider timeout handling
* Provider rate-limit handling
* Request correlation ID
* Configurable temperature-threshold webhook
* HTTP POST alert delivery
* Redis-backed webhook deduplication
* Best-effort webhook delivery
* Conditional webhook activation
* Configurable webhook connection and read timeouts
* Correlation ID propagation to webhook consumers
* Application health endpoint
* Docker multi-stage build
* Docker Compose local environment

## Architecture

WeatherBridge uses a lightweight Hexagonal Architecture, also known as Ports and Adapters Architecture.

The architecture separates business rules and use-case orchestration from external technologies such as Spring MVC, OpenWeather, Redis, HTTP webhooks, and Docker.

![WeatherBridge Hexagonal Architecture](docs/architecture/weatherbridge_hexagonal_architecture.png)

The primary dependency flow is:

```text
External Client
      ↓
Input Adapter
      ↓
Input Port
      ↓
Application Service
      ↓
Output Port
      ↓
Output Adapter
      ↓
External System
```

The current-weather flow can also trigger a secondary best-effort alert flow:

```text
WeatherController
        ↓
GetCurrentWeatherUseCase
        ↓
WeatherQueryService
        ↓
WeatherProviderPort
        ↓
OpenWeatherAdapter
        ↓
OpenWeather API
        ↓
CurrentWeather
        ↓
TemperatureAlertService
        ↓
WeatherAlertPort
        ↓
HttpWeatherAlertAdapter
        ↓
External Webhook Consumer
```

## Domain Layer

The domain layer contains business concepts and deterministic transformation rules.

Main components:

* `CurrentWeather`
* `WeatherForecast`
* `ForecastSlice`
* `DailyWeatherSummary`
* `FiveDayWeatherSummary`
* `Temperature`
* `Location`
* `ThermalSensation`
* `PrecipitationRisk`
* `ThermalSensationPolicy`
* `PrecipitationRiskPolicy`
* `ForecastAggregationService`
* `TemperatureThresholdPolicy`

The `TemperatureThresholdPolicy` determines whether a temperature alert must be generated.

The comparison is strictly greater than:

```text
temperatureCelsius > thresholdCelsius
```

A temperature equal to the configured threshold does not trigger an alert.

The domain layer does not depend directly on:

* Spring MVC
* Spring Cache
* HTTP clients
* Redis
* OpenWeather DTOs
* webhook DTOs
* Docker
* controller response classes

## Application Layer

The application layer coordinates use cases and depends on abstractions.

Input ports:

* `GetCurrentWeatherUseCase`
* `GetFiveDayWeatherUseCase`

Output ports:

* `WeatherProviderPort`
* `WeatherAlertPort`

Application services:

* `WeatherQueryService`
* `TemperatureAlertService`

`WeatherQueryService` coordinates current-weather and five-day forecast operations through `WeatherProviderPort`.

`TemperatureAlertService` evaluates the configured threshold after a successful current-weather query.

When the current temperature exceeds the threshold, the service creates a `TemperatureAlert` and delegates delivery to `WeatherAlertPort`.

Webhook delivery is treated as a best-effort side effect. A webhook failure is logged but does not invalidate an otherwise successful current-weather response.

## Output Adapters

Output adapters implement communication with external systems.

Main components:

### OpenWeather integration

* `OpenWeatherAdapter`
* `OpenWeatherMapper`
* `OpenWeatherForecastMapper`
* `OpenWeatherCurrentResponse`
* `OpenWeatherForecastResponse`

Responsibilities:

* call the OpenWeather endpoints;
* add API-key and query parameters;
* translate provider status codes;
* map provider DTOs to domain models;
* normalize units;
* isolate the application from provider-specific contracts.

### Temperature alert integration

* `HttpWeatherAlertAdapter`
* `NoOpWeatherAlertAdapter`
* `TemperatureAlertWebhookPayload`

Responsibilities:

* activate webhook delivery conditionally;
* send temperature alerts using HTTP POST;
* propagate the request correlation ID;
* apply connection and read timeouts;
* deduplicate successful alerts using Redis;
* avoid caching failed webhook deliveries;
* provide a no-operation implementation when alerts are disabled;
* prevent webhook failures from breaking weather responses.

## Temperature Alert Webhook

WeatherBridge can send an HTTP POST notification when the current temperature exceeds a configurable threshold.

The alert is evaluated after a successful current-weather lookup:

```text
Current weather obtained
        ↓
Threshold evaluated
        ↓
temperature > configured threshold?
        |
        +-- No  → no webhook
        |
        +-- Yes → send HTTP POST
```

The webhook is configured through `application.yml`:

```yaml
weather:
  alert:
    enabled: ${WEATHER_ALERT_ENABLED:false}
    threshold-celsius: ${WEATHER_ALERT_THRESHOLD_CELSIUS:35}
    webhook-url: ${WEATHER_ALERT_WEBHOOK_URL:}
    connect-timeout: ${WEATHER_ALERT_CONNECT_TIMEOUT:2s}
    read-timeout: ${WEATHER_ALERT_READ_TIMEOUT:5s}
```

Example payload:

```json
{
  "event": "TEMPERATURE_THRESHOLD_EXCEEDED",
  "location": {
    "city": "Macapá",
    "stateCode": "AP",
    "countryCode": "BR",
    "latitude": 0.0349,
    "longitude": -51.0694
  },
  "temperatureCelsius": 35.7,
  "feelsLikeCelsius": 41.2,
  "thresholdCelsius": 35.0,
  "observedAt": "2026-06-16T00:00:00Z",
  "correlationId": "030c6f12-6b78-4132-a4b0-b5ba39e56a15"
}
```

The threshold comparison is:

```text
temperatureCelsius > thresholdCelsius
```

A temperature equal to the threshold does not trigger an alert.

Webhook delivery is best-effort. Connection errors, timeouts, and non-successful HTTP responses are logged, but the current-weather endpoint still returns its successful response.

When alerts are disabled, `NoOpWeatherAlertAdapter` satisfies the `WeatherAlertPort` contract without performing an HTTP request.

## Caching Strategy

Caching is applied at two output-adapter boundaries.

### Weather provider cache

The following caches prevent redundant OpenWeather calls:

* `current-weather`
* `weather-forecast`

The provider cache stores normalized domain results instead of raw OpenWeather DTOs.

### Temperature alert cache

The `temperature-alert` cache prevents the same location and threshold from generating repeated webhook deliveries during the configured TTL.

Example deduplication key:

```text
macapa:ap:br:threshold:35
```

A successful webhook delivery is cached.

Failed deliveries are not cached, allowing a later weather request to attempt delivery again.

Default TTL values:

| Cache               | Default TTL |
| ------------------- | ----------: |
| `current-weather`   |  10 minutes |
| `weather-forecast`  |  30 minutes |
| `temperature-alert` |  30 minutes |

This means:

* use cases remain independent of Spring Cache;
* provider calls are cached;
* normalized domain results are reused;
* webhook alerts are deduplicated;
* failed webhook deliveries remain eligible for retry;
* each cache has an independent configurable TTL;
* external DTOs do not leak into cache contracts.

## Configure Environment

Create the local environment file:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
OPENWEATHER_API_KEY=your-real-api-key

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DATABASE=0

CURRENT_WEATHER_CACHE_TTL=10m
FORECAST_CACHE_TTL=30m

WEATHER_ALERT_ENABLED=false
WEATHER_ALERT_THRESHOLD_CELSIUS=35
WEATHER_ALERT_WEBHOOK_URL=
WEATHER_ALERT_CONNECT_TIMEOUT=2s
WEATHER_ALERT_READ_TIMEOUT=5s
WEATHER_ALERT_CACHE_TTL=30m
```

Do not commit `.env`.

Recommended `.gitignore` entries:

```gitignore
.env
.env.local
.env.*
!.env.example
```

For local webhook testing, with the API running in Docker and the webhook receiver running on macOS, use:

```dotenv
WEATHER_ALERT_ENABLED=true
WEATHER_ALERT_THRESHOLD_CELSIUS=20
WEATHER_ALERT_WEBHOOK_URL=http://host.docker.internal:9090/alerts
```

`host.docker.internal` allows the application container to reach the host machine.

## Observability

The application provides:

* request correlation IDs;
* structured contextual logs;
* operation duration;
* request parameters;
* provider failure classification;
* forecast aggregation logs;
* thermal-classification logs;
* precipitation-risk classification logs;
* temperature-threshold evaluation logs;
* webhook delivery lifecycle logs;
* webhook timeout and connection-failure logs;
* webhook HTTP status logs;
* webhook delivery duration;
* webhook correlation ID propagation;
* alert deduplication through Redis;
* Actuator health endpoint;
* Redis health indicator;
* metrics endpoint.

Actuator endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/caches
```

Sensitive configuration values must not be written to logs.

This includes:

* OpenWeather API keys;
* webhook URLs containing tokens;
* authorization headers;
* credentials embedded in URLs.

## Test the Temperature Alert Webhook

Start the local webhook receiver:

```bash
python3 webhook-receiver.py
```

Clear Redis before the test:

```bash
docker compose exec redis redis-cli FLUSHALL
```

Request current weather:

```bash
curl --silent --get \
  "http://localhost:8080/api/v1/weather/current" \
  --data-urlencode "city=Macapa" \
  --data-urlencode "stateCode=AP" \
  --data-urlencode "countryCode=BR" | jq
```

Inspect the alert cache:

```bash
docker compose exec redis \
  redis-cli --scan \
  --pattern "*temperature-alert*"
```

Expected key:

```text
weather-bridge::temperature-alert::macapa:ap:br:threshold:20
```

Run the same weather request again.

The first request should produce one webhook POST. The second request must not produce another POST while the deduplication TTL is active.

Verify the TTL:

```bash
docker compose exec redis \
  redis-cli TTL \
  "weather-bridge::temperature-alert::macapa:ap:br:threshold:20"
```

With the default alert TTL, the returned value should initially be close to:

```text
1800
```

To verify best-effort behavior:

1. stop the local webhook receiver;
2. clear Redis;
3. request current weather again;
4. confirm the endpoint still returns `200 OK`;
5. confirm the webhook failure appears in the application logs.

## Technical Presentation Guide

### Temperature Alert Webhook

Present the complete flow:

1. current weather is obtained;
2. `TemperatureAlertService` evaluates the threshold;
3. `TemperatureThresholdPolicy` applies the business rule;
4. `WeatherAlertPort` isolates the application from HTTP;
5. `HttpWeatherAlertAdapter` sends the POST;
6. Redis deduplicates successful deliveries;
7. the correlation ID is propagated;
8. webhook failures do not break the weather response.

Technical topics:

* conditional bean creation;
* `HttpWeatherAlertAdapter` versus `NoOpWeatherAlertAdapter`;
* configurable threshold;
* Spring `RestClient`;
* connection and read timeouts;
* best-effort side effects;
* Redis deduplication;
* alert cache-key design;
* retry eligibility after failure;
* synchronous-delivery tradeoff;
* separation between provider caching and alert deduplication.

Suggested live demonstration:

1. start the webhook receiver;
2. configure a low threshold;
3. clear Redis;
4. request current weather;
5. show the received payload;
6. repeat the request;
7. demonstrate that no second webhook is sent;
8. inspect the Redis key and TTL;
9. stop the webhook receiver;
10. demonstrate that the weather endpoint continues returning successfully.

## Testing Strategy

Recommended unit tests:

* `ThermalSensationPolicyTest`
* `PrecipitationRiskPolicyTest`
* `ForecastAggregationServiceTest`
* `TemperatureThresholdPolicyTest`
* `TemperatureAlertServiceTest`
* `WeatherQueryServiceTest`

Recommended web tests:

* `WeatherControllerTest`
* `GlobalExceptionHandlerTest`

Recommended OpenWeather adapter tests:

* successful current-weather mapping;
* successful forecast mapping;
* city not found;
* invalid API key;
* provider timeout;
* provider rate limit;
* malformed provider payload.

Recommended webhook adapter tests:

* successful webhook delivery;
* non-successful webhook response;
* webhook connection failure;
* webhook timeout;
* correlation ID propagation;
* alert deduplication;
* retry after failed delivery;
* disabled webhook using `NoOpWeatherAlertAdapter`.

Threshold scenarios:

* temperature below threshold;
* temperature equal to threshold;
* temperature above threshold;
* invalid non-finite threshold;
* threshold configuration change.

## Tradeoffs

* Redis adds infrastructure but enables shared caching and alert deduplication.
* Spring MVC keeps the execution model simple but holds a request thread during external calls.
* Webhook delivery is synchronous and adds latency when an alert is sent.
* Best-effort delivery favors weather availability over guaranteed notification.
* Redis deduplication prevents repeated alerts but does not provide durable event delivery.
* Failed webhook deliveries are retried only when another weather request occurs.
* No automatic provider retry avoids rate-limit amplification.
* No database is used because permanent persistence is outside the challenge scope.
* A single Maven module reduces structural complexity.
* Temperature and precipitation thresholds are application-defined business rules.
* Provider availability still affects cache misses.

## Important Notes

* The Dockerfile uses the official Maven image during the build stage.
* The project does not require `.mvn`, `mvnw`, or `mvnw.cmd`.
* The final image runs with Java 25 and a numeric non-root user.
* Docker Compose requires `OPENWEATHER_API_KEY`.
* The `.env` file must be supplied with `--env-file .env`.
* Current-weather and five-day summary endpoints are implemented.
* The temperature webhook is implemented and configurable.
* The webhook is triggered only by the current-weather use case.
* The five-day summary does not trigger webhook alerts.
* Successful webhook deliveries are deduplicated in Redis.
* Webhook failures do not fail the current-weather endpoint.
* The domain layer contains thermal-sensation, precipitation-risk, forecast-aggregation, and temperature-threshold rules.
* The five-day result is derived from three-hour forecast entries.
* Redis protects the provider from redundant calls and prevents duplicate alerts.
* Sensitive values must not appear in logs.
* `host.docker.internal` is intended only for local Docker-to-host testing.
* Detailed `TRACE` logging should be enabled only during investigation.
