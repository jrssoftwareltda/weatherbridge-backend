package com.weatherbridge.infrastructure.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "weather.cache")
public record WeatherCacheProperties(

        @NotNull
        Duration currentWeatherTtl,

        @NotNull
        Duration forecastTtl,

        @NotNull
        Duration alertDeduplicationTtl
) {

    public WeatherCacheProperties {
        validatePositive(
                currentWeatherTtl,
                "currentWeatherTtl"
        );

        validatePositive(
                forecastTtl,
                "forecastTtl"
        );

        validatePositive(
                alertDeduplicationTtl,
                "alertDeduplicationTtl"
        );
    }

    private static void validatePositive(
            Duration duration,
            String fieldName
    ) {
        if (duration == null) {
            return;
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }
    }
}