package com.weatherbridge.adapter.out.openweather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherForecastResponse(
        Integer cnt,
        List<ForecastItem> list,
        City city
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastItem(
            Long dt,
            MainData main,
            List<WeatherCondition> weather,
            Wind wind,
            Double pop,
            PrecipitationVolume rain,
            PrecipitationVolume snow
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainData(
            @JsonProperty("temp")
            Double temperature,

            @JsonProperty("feels_like")
            Double feelsLike,

            @JsonProperty("temp_min")
            Double minimumTemperature,

            @JsonProperty("temp_max")
            Double maximumTemperature,

            Integer pressure,
            Integer humidity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherCondition(
            Long id,
            String main,
            String description,
            String icon
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Wind(
            Double speed,
            Integer deg,
            Double gust
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrecipitationVolume(
            @JsonProperty("3h")
            Double threeHours
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(
            Long id,
            String name,
            Coordinates coord,
            String country,
            Integer timezone,
            Long sunrise,
            Long sunset
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinates(
            Double lat,
            Double lon
    ) {
    }
}