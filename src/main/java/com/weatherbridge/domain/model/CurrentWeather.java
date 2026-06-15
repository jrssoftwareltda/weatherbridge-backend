package com.weatherbridge.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CurrentWeather(
        Location location,
        Instant observedAt,
        Temperature temperature,
        int humidityPercent,
        int atmosphericPressureHpa,
        double windSpeedKmh,
        int cloudinessPercent,
        String condition,
        String description,
        WeatherSource source
) {

    public CurrentWeather {
        location = Objects.requireNonNull(
                location,
                "Location must not be null."
        );

        observedAt = Objects.requireNonNull(
                observedAt,
                "Observed time must not be null."
        );

        temperature = Objects.requireNonNull(
                temperature,
                "Temperature must not be null."
        );

        source = Objects.requireNonNull(
                source,
                "Weather source must not be null."
        );

        condition = requireText(
                condition,
                "condition"
        );

        description = requireText(
                description,
                "description"
        );

        if (humidityPercent < 0
                || humidityPercent > 100) {

            throw new IllegalArgumentException(
                    "Humidity must be between 0 and 100."
            );
        }

        if (atmosphericPressureHpa < 0) {
            throw new IllegalArgumentException(
                    "Atmospheric pressure must not be negative."
            );
        }

        if (!Double.isFinite(windSpeedKmh)
                || windSpeedKmh < 0) {

            throw new IllegalArgumentException(
                    "Wind speed must be a finite non-negative number."
            );
        }

        if (cloudinessPercent < 0
                || cloudinessPercent > 100) {

            throw new IllegalArgumentException(
                    "Cloudiness must be between 0 and 100."
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank."
            );
        }

        return value.trim();
    }
}