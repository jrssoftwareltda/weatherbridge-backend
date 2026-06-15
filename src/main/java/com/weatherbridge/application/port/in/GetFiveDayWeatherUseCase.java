package com.weatherbridge.application.port.in;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.FiveDayWeatherSummary;

public interface GetFiveDayWeatherUseCase {

    FiveDayWeatherSummary getFiveDaySummary(
            WeatherLocationQuery query
    );
}