package com.weatherbridge.application.port.out;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.domain.model.WeatherForecast;

public interface WeatherProviderPort {

    CurrentWeather getCurrentWeather(
            WeatherLocationQuery query
    );

    WeatherForecast getFiveDayForecast(
            WeatherLocationQuery query
    );
}