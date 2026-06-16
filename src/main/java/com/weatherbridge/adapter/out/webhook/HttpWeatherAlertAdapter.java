package com.weatherbridge.adapter.out.webhook;

import com.weatherbridge.application.exception.WeatherAlertDeliveryException;
import com.weatherbridge.application.model.TemperatureAlert;
import com.weatherbridge.application.model.WeatherAlertDelivery;
import com.weatherbridge.application.port.out.WeatherAlertPort;
import com.weatherbridge.infrastructure.properties.WeatherAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "weather.alert", name = "enabled", havingValue = "true")
public class HttpWeatherAlertAdapter implements WeatherAlertPort {
    private static final Logger log = LoggerFactory.getLogger(HttpWeatherAlertAdapter.class);
    private static final String ALERT_CACHE = "temperature-alert";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private final RestClient restClient;
    private final WeatherAlertProperties properties;
    private final Clock clock;

    public HttpWeatherAlertAdapter(@Qualifier("weatherAlertRestClient") RestClient restClient, WeatherAlertProperties properties, Clock clock) {
        this.restClient = Objects.requireNonNull(restClient, "Weather alert RestClient must not be null.");
        this.properties = Objects.requireNonNull(properties, "Weather alert properties must not be null.");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
        log.info("HTTP weather alert adapter initialized: " + "webhookHost={}, thresholdCelsius={}", properties.webhookHost(), properties.thresholdCelsius());
    }

    @Override
    @Cacheable(cacheNames = ALERT_CACHE, key = "#p0.deduplicationKey()", sync = true)
    public WeatherAlertDelivery send(TemperatureAlert alert) {
        Objects.requireNonNull(alert, "Temperature alert must not be null.");
        long startedAt = System.nanoTime();
        String correlationId = resolveCorrelationId();
        TemperatureAlertWebhookPayload payload = TemperatureAlertWebhookPayload.from(alert, correlationId);
        log.info("Sending temperature alert webhook: " + "city={}, stateCode={}, countryCode={}, " + "temperatureCelsius={}, " + "feelsLikeCelsius={}, " + "thresholdCelsius={}, " + "webhookHost={}, deduplicationKey={}", alert.location().city(), alert.location().stateCode(), alert.location().countryCode(), alert.temperatureCelsius(), alert.feelsLikeCelsius(), alert.thresholdCelsius(), properties.webhookHost(), alert.deduplicationKey());
        try {
            ResponseEntity<Void> response = restClient.post().uri(properties.webhookUri()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(CORRELATION_ID_HEADER, correlationId).body(payload).retrieve().toBodilessEntity();
            int statusCode = response.getStatusCode().value();
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new WeatherAlertDeliveryException("Temperature alert webhook returned " + "a non-successful HTTP status: " + statusCode + ".");
            }
            WeatherAlertDelivery delivery = WeatherAlertDelivery.delivered(statusCode, Instant.now(clock));
            log.info("Temperature alert webhook delivered: " + "city={}, statusCode={}, " + "completedAt={}, durationMs={}", alert.location().city(), delivery.statusCode(), delivery.completedAt(), elapsedMilliseconds(startedAt));
            return delivery;
        } catch (RestClientResponseException exception) {
            log.warn("Temperature alert webhook rejected: " + "city={}, statusCode={}, " + "webhookHost={}, durationMs={}", alert.location().city(), exception.getStatusCode().value(), properties.webhookHost(), elapsedMilliseconds(startedAt));
            log.debug("Temperature alert webhook rejection details", exception);
            throw new WeatherAlertDeliveryException("Temperature alert webhook returned " + "HTTP status " + exception.getStatusCode().value() + ".", exception);
        } catch (ResourceAccessException exception) {
            log.warn("Temperature alert webhook access failed: " + "city={}, webhookHost={}, " + "message={}, durationMs={}", alert.location().city(), properties.webhookHost(), exception.getMessage(), elapsedMilliseconds(startedAt));
            log.debug("Temperature alert webhook access failure details", exception);
            throw new WeatherAlertDeliveryException("Unable to access temperature alert webhook.", exception);
        } catch (RestClientException exception) {
            log.warn("Temperature alert webhook delivery failed: " + "city={}, webhookHost={}, " + "exceptionType={}, message={}, " + "durationMs={}", alert.location().city(), properties.webhookHost(), exception.getClass().getSimpleName(), exception.getMessage(), elapsedMilliseconds(startedAt));
            log.debug("Temperature alert webhook failure details", exception);
            throw new WeatherAlertDeliveryException("Unable to deliver temperature alert webhook.", exception);
        }
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            return "unavailable";
        }
        return correlationId;
    }

    private long elapsedMilliseconds(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}