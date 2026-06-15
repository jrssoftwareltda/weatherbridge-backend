package com.weatherbridge.adapter.out.openweather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherCurrentResponse(

        Coordinates coord,

        List<WeatherCondition> weather,

        MainData main,

        Wind wind,

        Clouds clouds,

        SystemData sys,

        Long dt,

        Integer timezone,

        String name
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinates(
            Double lon,
            Double lat
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
    public record Wind(
            Double speed,
            Integer deg,
            Double gust
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Clouds(
            Integer all
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemData(
            String country,
            Long sunrise,
            Long sunset
    ) {
    }
}