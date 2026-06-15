package com.weatherbridge.adapter.in.web.response;

import com.weatherbridge.domain.model.DailyWeatherSummary;
import com.weatherbridge.domain.model.FiveDayWeatherSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FiveDayWeatherResponse(
        LocationResponse location,
        Instant generatedAt,
        List<DailyWeatherResponse> days,
        String source
) {

    public static FiveDayWeatherResponse from(
            FiveDayWeatherSummary summary
    ) {
        List<DailyWeatherResponse> days =
                summary.days()
                        .stream()
                        .map(
                                DailyWeatherResponse::from
                        )
                        .toList();

        return new FiveDayWeatherResponse(
                new LocationResponse(
                        summary.location().city(),
                        summary.location().stateCode(),
                        summary.location().countryCode(),
                        summary.location().latitude(),
                        summary.location().longitude(),
                        summary.location()
                                .timezoneOffsetSeconds()
                ),
                summary.generatedAt(),
                days,
                summary.source().name()
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

    public record DailyWeatherResponse(
            LocalDate date,
            double minimumTemperatureCelsius,
            double maximumTemperatureCelsius,
            double averageTemperatureCelsius,
            double averageFeelsLikeCelsius,
            int averageHumidityPercent,
            double maximumPrecipitationProbabilityPercent,
            String precipitationRisk,
            double totalPrecipitationMillimeters,
            double maximumWindSpeedKmh,
            String dominantCondition
    ) {

        public static DailyWeatherResponse from(
                DailyWeatherSummary day
        ) {
            return new DailyWeatherResponse(
                    day.date(),
                    day.minimumTemperatureCelsius(),
                    day.maximumTemperatureCelsius(),
                    day.averageTemperatureCelsius(),
                    day.averageFeelsLikeCelsius(),
                    day.averageHumidityPercent(),
                    day.maximumPrecipitationProbabilityPercent(),
                    day.precipitationRisk().name(),
                    day.totalPrecipitationMillimeters(),
                    day.maximumWindSpeedKmh(),
                    day.dominantCondition()
            );
        }
    }
}