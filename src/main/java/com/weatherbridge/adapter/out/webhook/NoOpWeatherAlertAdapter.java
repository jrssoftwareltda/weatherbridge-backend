package com.weatherbridge.adapter.out.webhook;

import com.weatherbridge.application.model.TemperatureAlert;
import com.weatherbridge.application.model.WeatherAlertDelivery;
import com.weatherbridge.application.port.out.WeatherAlertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "weather.alert",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public final class NoOpWeatherAlertAdapter
        implements WeatherAlertPort {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NoOpWeatherAlertAdapter.class
            );

    private final Clock clock;

    public NoOpWeatherAlertAdapter(
            Clock clock
    ) {
        this.clock =
                Objects.requireNonNull(
                        clock,
                        "Clock must not be null."
                );

        log.info(
                "NoOp weather alert adapter initialized: "
                        + "temperature webhook is disabled"
        );
    }

    @Override
    public WeatherAlertDelivery send(
            TemperatureAlert alert
    ) {
        Objects.requireNonNull(
                alert,
                "Temperature alert must not be null."
        );

        log.debug(
                "Temperature alert delivery skipped: "
                        + "reason=FEATURE_DISABLED, "
                        + "city={}, stateCode={}, countryCode={}, "
                        + "temperatureCelsius={}, "
                        + "thresholdCelsius={}, "
                        + "deduplicationKey={}",
                alert.location().city(),
                alert.location().stateCode(),
                alert.location().countryCode(),
                alert.temperatureCelsius(),
                alert.thresholdCelsius(),
                alert.deduplicationKey()
        );

        return WeatherAlertDelivery.skipped(
                Instant.now(
                        clock
                )
        );
    }
}