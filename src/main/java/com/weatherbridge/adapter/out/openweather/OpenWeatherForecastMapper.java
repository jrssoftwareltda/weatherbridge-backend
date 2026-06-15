package com.weatherbridge.adapter.out.openweather;

import com.weatherbridge.adapter.out.openweather.dto.OpenWeatherForecastResponse;
import com.weatherbridge.application.exception.WeatherProviderException;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.domain.model.ForecastSlice;
import com.weatherbridge.domain.model.Location;
import com.weatherbridge.domain.model.WeatherForecast;
import com.weatherbridge.domain.model.WeatherSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class OpenWeatherForecastMapper {

    public WeatherForecast toDomain(
            OpenWeatherForecastResponse response,
            WeatherLocationQuery query
    ) {
        validateResponse(response);

        Location location = new Location(
                requiredText(
                        response.city().name(),
                        "city.name"
                ),
                query.stateCode(),
                resolveCountryCode(
                        response,
                        query
                ),
                requiredDouble(
                        response.city().coord().lat(),
                        "city.coord.lat"
                ),
                requiredDouble(
                        response.city().coord().lon(),
                        "city.coord.lon"
                ),
                requiredInteger(
                        response.city().timezone(),
                        "city.timezone"
                )
        );

        List<ForecastSlice> slices =
                response.list()
                        .stream()
                        .map(this::mapSlice)
                        .toList();

        return new WeatherForecast(
                location,
                slices,
                WeatherSource.OPEN_WEATHER
        );
    }

    private ForecastSlice mapSlice(
            OpenWeatherForecastResponse
                    .ForecastItem item
    ) {
        if (item == null) {
            throw invalidResponse(
                    "OpenWeather returned a null forecast item."
            );
        }

        if (item.main() == null) {
            throw invalidResponse(
                    "Forecast item does not contain main data."
            );
        }

        if (item.wind() == null) {
            throw invalidResponse(
                    "Forecast item does not contain wind data."
            );
        }

        OpenWeatherForecastResponse
                .WeatherCondition condition =
                getPrimaryCondition(item);

        double probabilityPercent =
                mapProbability(item.pop());

        double precipitationMillimeters =
                precipitationVolume(item.rain())
                        + precipitationVolume(
                        item.snow()
                );

        return new ForecastSlice(
                Instant.ofEpochSecond(
                        requiredLong(
                                item.dt(),
                                "list.dt"
                        )
                ),
                roundOneDecimal(
                        requiredDouble(
                                item.main().temperature(),
                                "list.main.temp"
                        )
                ),
                roundOneDecimal(
                        requiredDouble(
                                item.main()
                                        .minimumTemperature(),
                                "list.main.temp_min"
                        )
                ),
                roundOneDecimal(
                        requiredDouble(
                                item.main()
                                        .maximumTemperature(),
                                "list.main.temp_max"
                        )
                ),
                roundOneDecimal(
                        requiredDouble(
                                item.main().feelsLike(),
                                "list.main.feels_like"
                        )
                ),
                requiredInteger(
                        item.main().humidity(),
                        "list.main.humidity"
                ),
                roundOneDecimal(
                        probabilityPercent
                ),
                roundOneDecimal(
                        precipitationMillimeters
                ),
                roundOneDecimal(
                        requiredDouble(
                                item.wind().speed(),
                                "list.wind.speed"
                        ) * 3.6
                ),
                requiredText(
                        condition.main(),
                        "list.weather.main"
                )
        );
    }

    private OpenWeatherForecastResponse
            .WeatherCondition getPrimaryCondition(
            OpenWeatherForecastResponse
                    .ForecastItem item
    ) {
        if (item.weather() == null
                || item.weather().isEmpty()
                || item.weather().getFirst() == null) {

            throw invalidResponse(
                    "Forecast item does not contain weather conditions."
            );
        }

        return item.weather().getFirst();
    }

    private double mapProbability(
            Double probability
    ) {
        if (probability == null) {
            return 0;
        }

        if (!Double.isFinite(probability)
                || probability < 0
                || probability > 1) {

            throw invalidResponse(
                    "Forecast precipitation probability is invalid."
            );
        }

        return probability * 100;
    }

    private double precipitationVolume(
            OpenWeatherForecastResponse
                    .PrecipitationVolume volume
    ) {
        if (volume == null
                || volume.threeHours() == null) {

            return 0;
        }

        if (!Double.isFinite(volume.threeHours())
                || volume.threeHours() < 0) {

            throw invalidResponse(
                    "Forecast precipitation volume is invalid."
            );
        }

        return volume.threeHours();
    }

    private String resolveCountryCode(
            OpenWeatherForecastResponse response,
            WeatherLocationQuery query
    ) {
        if (response.city().country() == null
                || response.city().country().isBlank()) {

            return query.countryCode();
        }

        return response.city().country();
    }

    private void validateResponse(
            OpenWeatherForecastResponse response
    ) {
        if (response == null) {
            throw invalidResponse(
                    "OpenWeather returned an empty forecast response."
            );
        }

        if (response.city() == null) {
            throw invalidResponse(
                    "Forecast response does not contain city data."
            );
        }

        if (response.city().coord() == null) {
            throw invalidResponse(
                    "Forecast response does not contain coordinates."
            );
        }

        if (response.list() == null
                || response.list().isEmpty()) {

            throw invalidResponse(
                    "Forecast response does not contain forecast items."
            );
        }
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