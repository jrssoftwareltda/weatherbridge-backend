package com.weatherbridge.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record FiveDayWeatherSummary(
        Location location,
        Instant generatedAt,
        List<DailyWeatherSummary> days,
        WeatherSource source
) {

    public FiveDayWeatherSummary {
        location = Objects.requireNonNull(
                location,
                "Location must not be null."
        );

        generatedAt = Objects.requireNonNull(
                generatedAt,
                "Generated time must not be null."
        );

        source = Objects.requireNonNull(
                source,
                "Weather source must not be null."
        );

        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException(
                    "Daily summaries must not be empty."
            );
        }

        if (days.size() > 5) {
            throw new IllegalArgumentException(
                    "Five-day summary cannot contain more than five days."
            );
        }

        days = List.copyOf(days);
    }
}