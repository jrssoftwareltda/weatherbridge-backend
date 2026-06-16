package com.weatherbridge.application.service;

import com.weatherbridge.application.model.TemperatureAlert;
import com.weatherbridge.application.model.TemperatureAlertSettings;
import com.weatherbridge.application.model.WeatherAlertDelivery;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.out.WeatherAlertPort;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.domain.service.TemperatureThresholdPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public final class TemperatureAlertService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TemperatureAlertService.class
            );

    private final TemperatureAlertSettings
            settings;

    private final TemperatureThresholdPolicy
            thresholdPolicy;

    private final WeatherAlertPort
            weatherAlertPort;

    public TemperatureAlertService(
            TemperatureAlertSettings settings,
            TemperatureThresholdPolicy thresholdPolicy,
            WeatherAlertPort weatherAlertPort
    ) {
        this.settings =
                Objects.requireNonNull(
                        settings,
                        "Temperature alert settings must not be null."
                );

        this.thresholdPolicy =
                Objects.requireNonNull(
                        thresholdPolicy,
                        "Temperature threshold policy must not be null."
                );

        this.weatherAlertPort =
                Objects.requireNonNull(
                        weatherAlertPort,
                        "Weather alert port must not be null."
                );

        log.info(
                "TemperatureAlertService initialized: "
                        + "enabled={}, thresholdCelsius={}, "
                        + "alertPort={}",
                settings.enabled(),
                settings.thresholdCelsius(),
                weatherAlertPort
                        .getClass()
                        .getSimpleName()
        );
    }

    public void evaluate(
            CurrentWeather currentWeather,
            WeatherLocationQuery query
    ) {
        Objects.requireNonNull(
                currentWeather,
                "Current weather must not be null."
        );

        Objects.requireNonNull(
                query,
                "Weather location query must not be null."
        );

        long startedAt =
                System.nanoTime();

        String city =
                currentWeather
                        .location()
                        .city();

        double temperatureCelsius =
                currentWeather
                        .temperature()
                        .value();

        log.debug(
                "Starting temperature alert evaluation: "
                        + "city={}, temperatureCelsius={}, "
                        + "thresholdCelsius={}, enabled={}",
                city,
                temperatureCelsius,
                settings.thresholdCelsius(),
                settings.enabled()
        );

        if (!settings.enabled()) {
            log.debug(
                    "Temperature alert evaluation skipped: "
                            + "city={}, reason=FEATURE_DISABLED, "
                            + "durationMs={}",
                    city,
                    elapsedMilliseconds(
                            startedAt
                    )
            );

            return;
        }

        boolean thresholdExceeded =
                thresholdPolicy.isExceeded(
                        temperatureCelsius,
                        settings.thresholdCelsius()
                );

        if (!thresholdExceeded) {
            log.info(
                    "Temperature alert not required: "
                            + "city={}, temperatureCelsius={}, "
                            + "thresholdCelsius={}, durationMs={}",
                    city,
                    temperatureCelsius,
                    settings.thresholdCelsius(),
                    elapsedMilliseconds(
                            startedAt
                    )
            );

            return;
        }

        TemperatureAlert alert =
                new TemperatureAlert(
                        query.cacheKey(),
                        currentWeather.location(),
                        temperatureCelsius,
                        currentWeather
                                .temperature()
                                .feelsLike(),
                        settings.thresholdCelsius(),
                        currentWeather.observedAt()
                );

        log.warn(
                "Temperature threshold exceeded: "
                        + "city={}, stateCode={}, countryCode={}, "
                        + "temperatureCelsius={}, "
                        + "feelsLikeCelsius={}, "
                        + "thresholdCelsius={}, "
                        + "deduplicationKey={}",
                currentWeather.location().city(),
                currentWeather.location().stateCode(),
                currentWeather.location().countryCode(),
                alert.temperatureCelsius(),
                alert.feelsLikeCelsius(),
                alert.thresholdCelsius(),
                alert.deduplicationKey()
        );

        sendAlertSafely(
                alert,
                startedAt
        );
    }

    private void sendAlertSafely(
            TemperatureAlert alert,
            long startedAt
    ) {
        try {
            WeatherAlertDelivery delivery =
                    weatherAlertPort.send(
                            alert
                    );

            if (delivery.delivered()) {
                log.info(
                        "Temperature alert delivered successfully: "
                                + "city={}, statusCode={}, "
                                + "completedAt={}, durationMs={}",
                        alert.location().city(),
                        delivery.statusCode(),
                        delivery.completedAt(),
                        elapsedMilliseconds(
                                startedAt
                        )
                );

                return;
            }

            log.debug(
                    "Temperature alert delivery skipped: "
                            + "city={}, completedAt={}, durationMs={}",
                    alert.location().city(),
                    delivery.completedAt(),
                    elapsedMilliseconds(
                            startedAt
                    )
            );

        } catch (RuntimeException exception) {
            /*
             * O webhook é um efeito colateral best-effort.
             * Uma falha na entrega não deve transformar uma
             * consulta de clima válida em erro HTTP.
             */
            log.error(
                    "Temperature alert delivery failed "
                            + "without affecting weather response: "
                            + "city={}, temperatureCelsius={}, "
                            + "thresholdCelsius={}, "
                            + "exceptionType={}, message={}, "
                            + "durationMs={}",
                    alert.location().city(),
                    alert.temperatureCelsius(),
                    alert.thresholdCelsius(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    exception.getMessage(),
                    elapsedMilliseconds(
                            startedAt
                    )
            );

            log.debug(
                    "Temperature alert delivery failure details",
                    exception
            );
        }
    }

    private long elapsedMilliseconds(
            long startedAt
    ) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}
