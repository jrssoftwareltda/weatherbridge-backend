package com.weatherbridge.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record DailyWeatherSummary(
        LocalDate date,
        double minimumTemperatureCelsius,
        double maximumTemperatureCelsius,
        double averageTemperatureCelsius,
        double averageFeelsLikeCelsius,
        int averageHumidityPercent,
        double maximumPrecipitationProbabilityPercent,
        PrecipitationRisk precipitationRisk,
        double totalPrecipitationMillimeters,
        double maximumWindSpeedKmh,
        String dominantCondition
) {

    public DailyWeatherSummary {
        date = Objects.requireNonNull(
                date,
                "Summary date must not be null."
        );

        precipitationRisk = Objects.requireNonNull(
                precipitationRisk,
                "Precipitation risk must not be null."
        );

        if (dominantCondition == null
                || dominantCondition.isBlank()) {

            throw new IllegalArgumentException(
                    "Dominant condition must not be blank."
            );
        }

        dominantCondition =
                dominantCondition.trim();
    }
}