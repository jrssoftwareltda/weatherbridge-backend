package com.weatherbridge.application.service;

import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.in.GetCurrentWeatherUseCase;
import com.weatherbridge.application.port.in.GetFiveDayWeatherUseCase;
import com.weatherbridge.application.port.out.WeatherProviderPort;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.domain.model.FiveDayWeatherSummary;
import com.weatherbridge.domain.model.WeatherForecast;
import com.weatherbridge.domain.service.ForecastAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherQueryService
        implements GetCurrentWeatherUseCase,
        GetFiveDayWeatherUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WeatherQueryService.class
            );

    private final WeatherProviderPort
            weatherProviderPort;

    private final ForecastAggregationService
            forecastAggregationService;

    private final TemperatureAlertService
            temperatureAlertService;

    private final Clock clock;

    public WeatherQueryService(
            WeatherProviderPort weatherProviderPort,
            ForecastAggregationService forecastAggregationService,
            TemperatureAlertService temperatureAlertService,
            Clock clock
    ) {
        this.weatherProviderPort =
                Objects.requireNonNull(
                        weatherProviderPort,
                        "Weather provider port must not be null."
                );

        this.forecastAggregationService =
                Objects.requireNonNull(
                        forecastAggregationService,
                        "Forecast aggregation service must not be null."
                );

        this.temperatureAlertService =
                Objects.requireNonNull(
                        temperatureAlertService,
                        "Temperature alert service must not be null."
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "Clock must not be null."
                );

        log.debug(
                "WeatherQueryService initialized: "
                        + "provider={}, forecastAggregationService={}, "
                        + "temperatureAlertService={}",
                weatherProviderPort
                        .getClass()
                        .getSimpleName(),
                forecastAggregationService
                        .getClass()
                        .getSimpleName(),
                temperatureAlertService
                        .getClass()
                        .getSimpleName()
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

        long startedAt =
                System.nanoTime();

        log.info(
                "Starting current weather query: "
                        + "city={}, stateCode={}, countryCode={}",
                query.city(),
                query.stateCode(),
                query.countryCode()
        );

        try {
            log.debug(
                    "Calling current weather provider: "
                            + "providerQuery={}, cacheKey={}",
                    query.toProviderQuery(),
                    query.cacheKey()
            );

            CurrentWeather weather =
                    weatherProviderPort
                            .getCurrentWeather(
                                    query
                            );

            if (weather == null) {
                throw new IllegalStateException(
                        "Weather provider returned a null "
                                + "current weather result."
                );
            }

            log.debug(
                    "Current weather provider returned: "
                            + "city={}, temperatureCelsius={}, "
                            + "feelsLikeCelsius={}, condition={}, "
                            + "observedAt={}, source={}",
                    weather.location().city(),
                    weather.temperature().value(),
                    weather.temperature().feelsLike(),
                    weather.condition(),
                    weather.observedAt(),
                    weather.source()
            );

            evaluateTemperatureAlert(
                    weather,
                    query
            );

            log.info(
                    "Current weather query completed: "
                            + "city={}, temperatureCelsius={}, "
                            + "condition={}, source={}, durationMs={}",
                    weather.location().city(),
                    weather.temperature().value(),
                    weather.condition(),
                    weather.source(),
                    elapsedMilliseconds(
                            startedAt
                    )
            );

            return weather;

        } catch (RuntimeException exception) {
            log.error(
                    "Current weather query failed: "
                            + "city={}, stateCode={}, countryCode={}, "
                            + "exceptionType={}, message={}, durationMs={}",
                    query.city(),
                    query.stateCode(),
                    query.countryCode(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(
                            startedAt
                    ),
                    exception
            );

            throw exception;
        }
    }

    @Override
    public FiveDayWeatherSummary getFiveDaySummary(
            WeatherLocationQuery query
    ) {
        Objects.requireNonNull(
                query,
                "Weather location query must not be null."
        );

        long startedAt =
                System.nanoTime();

        log.info(
                "Starting five-day weather summary query: "
                        + "city={}, stateCode={}, countryCode={}",
                query.city(),
                query.stateCode(),
                query.countryCode()
        );

        try {
            log.debug(
                    "Calling forecast provider: "
                            + "providerQuery={}, cacheKey={}",
                    query.toProviderQuery(),
                    query.cacheKey()
            );

            WeatherForecast forecast =
                    weatherProviderPort
                            .getFiveDayForecast(
                                    query
                            );

            if (forecast == null) {
                throw new IllegalStateException(
                        "Weather provider returned a null forecast."
                );
            }

            log.debug(
                    "Forecast provider returned: "
                            + "city={}, slices={}, "
                            + "timezoneOffsetSeconds={}, source={}",
                    forecast.location().city(),
                    forecast.slices().size(),
                    forecast.location()
                            .timezoneOffsetSeconds(),
                    forecast.source()
            );

            FiveDayWeatherSummary summary =
                    forecastAggregationService.aggregate(
                            forecast,
                            Instant.now(
                                    clock
                            )
                    );

            log.info(
                    "Five-day weather summary completed: "
                            + "city={}, days={}, source={}, durationMs={}",
                    summary.location().city(),
                    summary.days().size(),
                    summary.source(),
                    elapsedMilliseconds(
                            startedAt
                    )
            );

            return summary;

        } catch (RuntimeException exception) {
            log.error(
                    "Five-day weather summary failed: "
                            + "city={}, stateCode={}, countryCode={}, "
                            + "exceptionType={}, message={}, durationMs={}",
                    query.city(),
                    query.stateCode(),
                    query.countryCode(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(
                            startedAt
                    ),
                    exception
            );

            throw exception;
        }
    }

    private void evaluateTemperatureAlert(
            CurrentWeather weather,
            WeatherLocationQuery query
    ) {
        log.debug(
                "Starting temperature alert evaluation: "
                        + "city={}, temperatureCelsius={}, cacheKey={}",
                weather.location().city(),
                weather.temperature().value(),
                query.cacheKey()
        );

        /*
         * TemperatureAlertService trata a entrega como best-effort.
         * Uma falha no webhook não deve invalidar a consulta de clima.
         */
        temperatureAlertService.evaluate(
                weather,
                query
        );

        log.debug(
                "Temperature alert evaluation completed: "
                        + "city={}, temperatureCelsius={}",
                weather.location().city(),
                weather.temperature().value()
        );
    }

    private long elapsedMilliseconds(
            long startedAt
    ) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}