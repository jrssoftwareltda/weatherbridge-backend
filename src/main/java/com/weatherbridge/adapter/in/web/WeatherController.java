package com.weatherbridge.adapter.in.web;

import com.weatherbridge.adapter.in.web.response.CurrentWeatherResponse;
import com.weatherbridge.adapter.in.web.response.FiveDayWeatherResponse;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.in.GetCurrentWeatherUseCase;
import com.weatherbridge.application.port.in.GetFiveDayWeatherUseCase;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.domain.model.FiveDayWeatherSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WeatherController.class
            );

    private final GetCurrentWeatherUseCase
            getCurrentWeatherUseCase;

    private final GetFiveDayWeatherUseCase
            getFiveDayWeatherUseCase;

    public WeatherController(
            GetCurrentWeatherUseCase
                    getCurrentWeatherUseCase,
            GetFiveDayWeatherUseCase
                    getFiveDayWeatherUseCase
    ) {
        this.getCurrentWeatherUseCase =
                Objects.requireNonNull(
                        getCurrentWeatherUseCase,
                        "Get current weather use case must not be null."
                );

        this.getFiveDayWeatherUseCase =
                Objects.requireNonNull(
                        getFiveDayWeatherUseCase,
                        "Get five-day weather use case must not be null."
                );

        log.debug(
                "WeatherController initialized"
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
        long startedAt = System.nanoTime();

        log.info(
                "Received current weather HTTP request: "
                        + "city={}, stateCode={}, countryCode={}",
                city,
                stateCode,
                countryCode
        );

        try {
            WeatherLocationQuery query =
                    createQuery(
                            city,
                            stateCode,
                            countryCode
                    );

            CurrentWeather weather =
                    getCurrentWeatherUseCase
                            .getCurrentWeather(query);

            CurrentWeatherResponse response =
                    CurrentWeatherResponse.from(
                            weather
                    );

            log.info(
                    "Current weather HTTP request completed: "
                            + "city={}, status=200, durationMs={}",
                    response.location().city(),
                    elapsedMilliseconds(startedAt)
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException exception) {
            log.warn(
                    "Current weather HTTP request failed: "
                            + "city={}, exceptionType={}, "
                            + "message={}, durationMs={}",
                    city,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(startedAt)
            );

            throw exception;
        }
    }

    @GetMapping("/forecast/5-days")
    public ResponseEntity<FiveDayWeatherResponse>
    getFiveDayForecast(
            @RequestParam
            String city,

            @RequestParam(required = false)
            String stateCode,

            @RequestParam(required = false)
            String countryCode
    ) {
        long startedAt = System.nanoTime();

        log.info(
                "Received five-day forecast HTTP request: "
                        + "city={}, stateCode={}, countryCode={}",
                city,
                stateCode,
                countryCode
        );

        try {
            WeatherLocationQuery query =
                    createQuery(
                            city,
                            stateCode,
                            countryCode
                    );

            log.debug(
                    "Invoking five-day weather use case: "
                            + "providerQuery={}, cacheKey={}",
                    query.toProviderQuery(),
                    query.cacheKey()
            );

            FiveDayWeatherSummary summary =
                    getFiveDayWeatherUseCase
                            .getFiveDaySummary(query);

            FiveDayWeatherResponse response =
                    FiveDayWeatherResponse.from(
                            summary
                    );

            log.info(
                    "Five-day forecast HTTP request completed: "
                            + "city={}, days={}, status=200, durationMs={}",
                    response.location().city(),
                    response.days().size(),
                    elapsedMilliseconds(startedAt)
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException exception) {
            log.warn(
                    "Five-day forecast HTTP request failed: "
                            + "city={}, stateCode={}, countryCode={}, "
                            + "exceptionType={}, message={}, durationMs={}",
                    city,
                    stateCode,
                    countryCode,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(startedAt)
            );

            throw exception;
        }
    }

    private WeatherLocationQuery createQuery(
            String city,
            String stateCode,
            String countryCode
    ) {
        WeatherLocationQuery query =
                new WeatherLocationQuery(
                        city,
                        stateCode,
                        countryCode
                );

        log.debug(
                "Weather query normalized: "
                        + "city={}, stateCode={}, countryCode={}, "
                        + "providerQuery={}, cacheKey={}",
                query.city(),
                query.stateCode(),
                query.countryCode(),
                query.toProviderQuery(),
                query.cacheKey()
        );

        return query;
    }

    private long elapsedMilliseconds(
            long startedAt
    ) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}