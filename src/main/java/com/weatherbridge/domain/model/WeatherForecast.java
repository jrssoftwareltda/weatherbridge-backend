package com.weatherbridge.domain.model;

import java.util.List;
import java.util.Objects;

public record WeatherForecast(
        Location location,
        List<ForecastSlice> slices,
        WeatherSource source
) {

    public WeatherForecast {
        location = Objects.requireNonNull(
                location,
                "Location must not be null."
        );

        source = Objects.requireNonNull(
                source,
                "Weather source must not be null."
        );

        if (slices == null || slices.isEmpty()) {
            throw new IllegalArgumentException(
                    "Forecast slices must not be empty."
            );
        }

        slices = List.copyOf(slices);
    }
}