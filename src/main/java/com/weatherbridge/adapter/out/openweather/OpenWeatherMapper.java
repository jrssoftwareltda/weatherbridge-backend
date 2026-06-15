package com.weatherbridge.adapter.out.openweather;

import com.weatherbridge.adapter.out.openweather.dto.OpenWeatherCurrentResponse;
import com.weatherbridge.application.exception.WeatherProviderException;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.domain.model.Location;
import com.weatherbridge.domain.model.Temperature;
import com.weatherbridge.domain.model.ThermalSensation;
import com.weatherbridge.domain.model.WeatherSource;
import com.weatherbridge.domain.service.ThermalSensationPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class OpenWeatherMapper {

    private final ThermalSensationPolicy thermalSensationPolicy;

    public OpenWeatherMapper(
            ThermalSensationPolicy thermalSensationPolicy
    ) {
        this.thermalSensationPolicy =
                thermalSensationPolicy;
    }

    public CurrentWeather toDomain(
            OpenWeatherCurrentResponse response,
            WeatherLocationQuery query
    ) {
        validateResponse(response);

        OpenWeatherCurrentResponse.WeatherCondition
                primaryCondition =
                getPrimaryCondition(
                        response.weather()
                );

        double temperatureCelsius =
                requiredDouble(
                        response.main().temperature(),
                        "main.temp"
                );

        double feelsLikeCelsius =
                requiredDouble(
                        response.main().feelsLike(),
                        "main.feels_like"
                );

        ThermalSensation category =
                thermalSensationPolicy.classify(
                        feelsLikeCelsius
                );

        Location location = new Location(
                requiredText(
                        response.name(),
                        "name"
                ),
                query.stateCode(),
                resolveCountryCode(
                        response,
                        query
                ),
                requiredDouble(
                        response.coord().lat(),
                        "coord.lat"
                ),
                requiredDouble(
                        response.coord().lon(),
                        "coord.lon"
                ),
                requiredInteger(
                        response.timezone(),
                        "timezone"
                )
        );

        Temperature temperature =
                Temperature.celsius(
                        roundOneDecimal(
                                temperatureCelsius
                        ),
                        roundOneDecimal(
                                feelsLikeCelsius
                        ),
                        category
                );

        return new CurrentWeather(
                location,
                Instant.ofEpochSecond(
                        requiredLong(
                                response.dt(),
                                "dt"
                        )
                ),
                temperature,
                requiredInteger(
                        response.main().humidity(),
                        "main.humidity"
                ),
                requiredInteger(
                        response.main().pressure(),
                        "main.pressure"
                ),
                roundOneDecimal(
                        convertMetersPerSecondToKmh(
                                requiredDouble(
                                        response.wind().speed(),
                                        "wind.speed"
                                )
                        )
                ),
                requiredInteger(
                        response.clouds().all(),
                        "clouds.all"
                ),
                requiredText(
                        primaryCondition.main(),
                        "weather.main"
                ),
                requiredText(
                        primaryCondition.description(),
                        "weather.description"
                ),
                WeatherSource.OPEN_WEATHER
        );
    }

    private void validateResponse(
            OpenWeatherCurrentResponse response
    ) {
        if (response == null) {
            throw invalidResponse(
                    "OpenWeather returned an empty response."
            );
        }

        if (response.coord() == null) {
            throw invalidResponse(
                    "OpenWeather response does not contain coordinates."
            );
        }

        if (response.main() == null) {
            throw invalidResponse(
                    "OpenWeather response does not contain temperature data."
            );
        }

        if (response.wind() == null) {
            throw invalidResponse(
                    "OpenWeather response does not contain wind data."
            );
        }

        if (response.clouds() == null) {
            throw invalidResponse(
                    "OpenWeather response does not contain cloud data."
            );
        }
    }

    private OpenWeatherCurrentResponse.WeatherCondition
    getPrimaryCondition(
            List<OpenWeatherCurrentResponse.WeatherCondition>
                    conditions
    ) {
        if (conditions == null || conditions.isEmpty()) {
            throw invalidResponse(
                    "OpenWeather response does not contain weather conditions."
            );
        }

        OpenWeatherCurrentResponse.WeatherCondition
                condition = conditions.getFirst();

        if (condition == null) {
            throw invalidResponse(
                    "OpenWeather returned an invalid weather condition."
            );
        }

        return condition;
    }

    private String resolveCountryCode(
            OpenWeatherCurrentResponse response,
            WeatherLocationQuery query
    ) {
        if (response.sys() == null
                || response.sys().country() == null
                || response.sys().country().isBlank()) {

            return query.countryCode();
        }

        return response.sys().country();
    }

    private double convertMetersPerSecondToKmh(
            double metersPerSecond
    ) {
        return metersPerSecond * 3.6;
    }

    private double roundOneDecimal(
            double value
    ) {
        return BigDecimal
                .valueOf(value)
                .setScale(
                        1,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }

    private String requiredText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw invalidResponse(
                    "Required field is missing: "
                            + fieldName
            );
        }

        return value.trim();
    }

    private double requiredDouble(
            Double value,
            String fieldName
    ) {
        if (value == null
                || !Double.isFinite(value)) {

            throw invalidResponse(
                    "Required numeric field is invalid: "
                            + fieldName
            );
        }

        return value;
    }

    private int requiredInteger(
            Integer value,
            String fieldName
    ) {
        if (value == null) {
            throw invalidResponse(
                    "Required integer field is missing: "
                            + fieldName
            );
        }

        return value;
    }

    private long requiredLong(
            Long value,
            String fieldName
    ) {
        if (value == null) {
            throw invalidResponse(
                    "Required long field is missing: "
                            + fieldName
            );
        }

        return value;
    }

    private WeatherProviderException invalidResponse(
            String message
    ) {
        return new WeatherProviderException(
                WeatherProviderException.FailureType
                        .INVALID_RESPONSE,
                message
        );
    }
}