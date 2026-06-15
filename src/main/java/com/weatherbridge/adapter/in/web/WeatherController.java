package com.weatherbridge.adapter.in.web;

import com.weatherbridge.adapter.in.web.response.CurrentWeatherResponse;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.in.GetCurrentWeatherUseCase;
import com.weatherbridge.domain.model.CurrentWeather;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final GetCurrentWeatherUseCase getCurrentWeatherUseCase;

    public WeatherController(
            GetCurrentWeatherUseCase getCurrentWeatherUseCase
    ) {
        this.getCurrentWeatherUseCase =
                Objects.requireNonNull(
                        getCurrentWeatherUseCase
                );
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentWeatherResponse>
    getCurrentWeather(
            @RequestParam
            String city,

            @RequestParam(required = false)
            String stateCode,

            @RequestParam(required = false)
            String countryCode
    ) {
        WeatherLocationQuery query =
                new WeatherLocationQuery(
                        city,
                        stateCode,
                        countryCode
                );

        CurrentWeather weather =
                getCurrentWeatherUseCase
                        .getCurrentWeather(query);

        return ResponseEntity.ok(
                CurrentWeatherResponse.from(weather)
        );
    }
}