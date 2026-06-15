package com.weatherbridge.domain.model;

import java.time.Instant;
import java.util.Objects;

public record ForecastSlice(
        Instant forecastAt,
        double temperatureCelsius,
        double minimumTemperatureCelsius,
        double maximumTemperatureCelsius,
        double feelsLikeCelsius,
        int humidityPercent,
        double precipitationProbabilityPercent,
        double precipitationMillimeters,
        double windSpeedKmh,
        String condition
) {

    public ForecastSlice {
        forecastAt = Objects.requireNonNull(
                forecastAt,
                "Forecast time must not be null."
        );

        validateFinite(
                temperatureCelsius,
                "temperatureCelsius"
        );

        validateFinite(
                minimumTemperatureCelsius,
                "minimumTemperatureCelsius"
        );

        validateFinite(
                maximumTemperatureCelsius,
                "maximumTemperatureCelsius"
        );

        validateFinite(
                feelsLikeCelsius,
                "feelsLikeCelsius"
        );

        validateFinite(
                precipitationProbabilityPercent,
                "precipitationProbabilityPercent"
        );

        validateFinite(
                precipitationMillimeters,
                "precipitationMillimeters"
        );

        validateFinite(
                windSpeedKmh,
                "windSpeedKmh"
        );

        if (humidityPercent < 0
                || humidityPercent > 100) {

            throw new IllegalArgumentException(
                    "Humidity must be between 0 and 100."
            );
        }

        if (precipitationProbabilityPercent < 0
                || precipitationProbabilityPercent > 100) {

            throw new IllegalArgumentException(
                    "Precipitation probability must be between 0 and 100."
            );
        }

        if (precipitationMillimeters < 0) {
            throw new IllegalArgumentException(
                    "Precipitation must not be negative."
            );
        }

        if (windSpeedKmh < 0) {
            throw new IllegalArgumentException(
                    "Wind speed must not be negative."
            );
        }

        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException(
                    "Weather condition must not be blank."
            );
        }

        condition = condition.trim();
    }

    private static void validateFinite(
            double value,
            String fieldName
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    fieldName + " must be a finite number."
            );
        }
    }
}