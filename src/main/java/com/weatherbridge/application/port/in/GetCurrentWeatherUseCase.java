package com.weatherbridge.application.port.in;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.CurrentWeather;

public interface GetCurrentWeatherUseCase {

    CurrentWeather getCurrentWeather(
            WeatherLocationQuery query
    );
}