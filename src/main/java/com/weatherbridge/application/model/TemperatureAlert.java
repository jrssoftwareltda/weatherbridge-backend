package com.weatherbridge.application.model;

import com.weatherbridge.domain.model.Location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TemperatureAlert(
        String locationCacheKey,
        Location location,
        double temperatureCelsius,
        double feelsLikeCelsius,
        double thresholdCelsius,
        Instant observedAt
) {

    public TemperatureAlert {
        if (locationCacheKey == null
                || locationCacheKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Location cache key must not be blank."
            );
        }

        locationCacheKey =
                locationCacheKey.trim();

        Objects.requireNonNull(
                location,
                "Alert location must not be null."
        );

        Objects.requireNonNull(
                observedAt,
                "Alert observation time must not be null."
        );

        validateFinite(
                temperatureCelsius,
                "Temperature"
        );

        validateFinite(
                feelsLikeCelsius,
                "Feels-like temperature"
        );

        validateFinite(
                thresholdCelsius,
                "Temperature threshold"
        );
    }

    public String deduplicationKey() {
        String normalizedThreshold =
                BigDecimal
                        .valueOf(thresholdCelsius)
                        .stripTrailingZeros()
                        .toPlainString();

        return locationCacheKey
                + ":threshold:"
                + normalizedThreshold;
    }

    private static void validateFinite(
            double value,
            String fieldName
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a finite number."
            );
        }
    }
}