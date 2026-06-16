package com.weatherbridge.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TemperatureThresholdPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TemperatureThresholdPolicy.class
            );

    public boolean isExceeded(
            double temperatureCelsius,
            double thresholdCelsius
    ) {
        validateFinite(
                temperatureCelsius,
                "Temperature"
        );

        validateFinite(
                thresholdCelsius,
                "Temperature threshold"
        );

        boolean exceeded =
                temperatureCelsius > thresholdCelsius;

        log.debug(
                "Temperature threshold evaluated: "
                        + "temperatureCelsius={}, "
                        + "thresholdCelsius={}, exceeded={}",
                temperatureCelsius,
                thresholdCelsius,
                exceeded
        );

        return exceeded;
    }

    private void validateFinite(
            double value,
            String fieldName
    ) {
        if (!Double.isFinite(value)) {
            log.warn(
                    "Invalid temperature value: "
                            + "field={}, value={}, reason=NON_FINITE",
                    fieldName,
                    value
            );

            throw new IllegalArgumentException(
                    fieldName
                            + " must be a finite number."
            );
        }
    }
}