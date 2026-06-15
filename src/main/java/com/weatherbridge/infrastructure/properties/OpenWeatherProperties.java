package com.weatherbridge.infrastructure.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "open-weather")
public record OpenWeatherProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String apiKey,

        @NotBlank
        String units,

        @NotBlank
        String language,

        @NotNull
        Duration connectTimeout,

        @NotNull
        Duration readTimeout
) {

    public OpenWeatherProperties {
        if (connectTimeout != null
                && (connectTimeout.isZero()
                || connectTimeout.isNegative())) {

            throw new IllegalArgumentException(
                    "OpenWeather connect timeout must be positive."
            );
        }

        if (readTimeout != null
                && (readTimeout.isZero()
                || readTimeout.isNegative())) {

            throw new IllegalArgumentException(
                    "OpenWeather read timeout must be positive."
            );
        }
    }
}