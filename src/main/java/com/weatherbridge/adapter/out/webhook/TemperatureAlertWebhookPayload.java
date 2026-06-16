package com.weatherbridge.adapter.out.webhook;

import com.weatherbridge.application.model.TemperatureAlert;

import java.time.Instant;
import java.util.Objects;

public record TemperatureAlertWebhookPayload(String event, LocationPayload location, double temperatureCelsius,
                                             double feelsLikeCelsius, double thresholdCelsius, Instant observedAt,
                                             String correlationId) {
    private static final String EVENT_NAME = "TEMPERATURE_THRESHOLD_EXCEEDED";

    public TemperatureAlertWebhookPayload {
        if (event == null || event.isBlank()) {
            throw new IllegalArgumentException("Webhook event must not be blank.");
        }
        Objects.requireNonNull(location, "Webhook location must not be null.");
        Objects.requireNonNull(observedAt, "Webhook observation time must not be null.");
        correlationId = correlationId == null || correlationId.isBlank() ? "unavailable" : correlationId.trim();
    }

    public static TemperatureAlertWebhookPayload from(TemperatureAlert alert, String correlationId) {
        Objects.requireNonNull(alert, "Temperature alert must not be null.");
        return new TemperatureAlertWebhookPayload(EVENT_NAME, new LocationPayload(alert.location().city(), alert.location().stateCode(), alert.location().countryCode(), alert.location().latitude(), alert.location().longitude()), alert.temperatureCelsius(), alert.feelsLikeCelsius(), alert.thresholdCelsius(), alert.observedAt(), correlationId);
    }

    public record LocationPayload(String city, String stateCode, String countryCode, double latitude,
                                  double longitude) {
        public LocationPayload {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("Webhook city must not be blank.");
            }
            if (countryCode == null || countryCode.isBlank()) {
                throw new IllegalArgumentException("Webhook country code must not be blank.");
            }
        }
    }
}