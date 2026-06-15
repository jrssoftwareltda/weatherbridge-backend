package com.weatherbridge.application.port.out;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.CurrentWeather;

public interface WeatherProviderPort {

    CurrentWeather getCurrentWeather(
            WeatherLocationQuery query
    );
}