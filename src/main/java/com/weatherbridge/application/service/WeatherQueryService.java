package com.weatherbridge.application.service;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.in.GetCurrentWeatherUseCase;
import com.weatherbridge.application.port.out.WeatherProviderPort;
import com.weatherbridge.domain.model.CurrentWeather;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class WeatherQueryService
        implements GetCurrentWeatherUseCase {

    private final WeatherProviderPort weatherProviderPort;

    public WeatherQueryService(
            WeatherProviderPort weatherProviderPort
    ) {
        this.weatherProviderPort =
                Objects.requireNonNull(
                        weatherProviderPort,
                        "Weather provider port must not be null."
                );
    }

    @Override
    public CurrentWeather getCurrentWeather(
            WeatherLocationQuery query
    ) {
        Objects.requireNonNull(
                query,
                "Weather location query must not be null."
        );

        return weatherProviderPort.getCurrentWeather(
                query
        );
    }
}