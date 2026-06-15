package com.weatherbridge.adapter.in.web.response;

import com.weatherbridge.domain.model.CurrentWeather;

import java.time.Instant;

public record CurrentWeatherResponse(
        LocationResponse location,
        Instant observedAt,
        TemperatureResponse temperature,
        int humidityPercent,
        int atmosphericPressureHpa,
        double windSpeedKmh,
        int cloudinessPercent,
        String condition,
        String description,
        String source
) {

    public static CurrentWeatherResponse from(
            CurrentWeather weather
    ) {
        return new CurrentWeatherResponse(
                new LocationResponse(
                        weather.location().city(),
                        weather.location().stateCode(),
                        weather.location().countryCode(),
                        weather.location().latitude(),
                        weather.location().longitude(),
                        weather.location().timezoneOffsetSeconds()
                ),
                weather.observedAt(),
                new TemperatureResponse(
                        weather.temperature().value(),
                        weather.temperature().feelsLike(),
                        weather.temperature().difference(),
                        weather.temperature().unit().name(),
                        weather.temperature().category().name()
                ),
                weather.humidityPercent(),
                weather.atmosphericPressureHpa(),
                weather.windSpeedKmh(),
                weather.cloudinessPercent(),
                weather.condition(),
                weather.description(),
                weather.source().name()
        );
    }

    public record LocationResponse(
            String city,
            String stateCode,
            String countryCode,
            double latitude,
            double longitude,
            int timezoneOffsetSeconds
    ) {
    }

    public record TemperatureResponse(
            double value,
            double feelsLike,
            double difference,
            String unit,
            String category
    ) {
    }
}