package com.weatherbridge.domain.service;

import com.weatherbridge.domain.model.ThermalSensation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThermalSensationPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ThermalSensationPolicy.class
            );

    public ThermalSensation classify(
            double feelsLikeCelsius
    ) {
        log.debug(
                "Classifying thermal sensation: feelsLikeCelsius={}",
                feelsLikeCelsius
        );

        validateTemperature(
                feelsLikeCelsius
        );

        ThermalSensation thermalSensation =
                determineThermalSensation(
                        feelsLikeCelsius
                );

        log.debug(
                "Thermal sensation classified successfully: "
                        + "feelsLikeCelsius={}, thermalSensation={}",
                feelsLikeCelsius,
                thermalSensation
        );

        return thermalSensation;
    }

    private void validateTemperature(
            double feelsLikeCelsius
    ) {
        if (!Double.isFinite(feelsLikeCelsius)) {
            log.warn(
                    "Invalid feels-like temperature: "
                            + "value={}, reason=NON_FINITE_VALUE",
                    feelsLikeCelsius
            );

            throw new IllegalArgumentException(
                    "Feels-like temperature must be a finite number."
            );
        }

        log.trace(
                "Feels-like temperature validated: "
                        + "feelsLikeCelsius={}",
                feelsLikeCelsius
        );
    }

    private ThermalSensation determineThermalSensation(
            double feelsLikeCelsius
    ) {
        if (feelsLikeCelsius < 5) {
            log.trace(
                    "Feels-like temperature matched VERY_COLD range: "
                            + "feelsLikeCelsius={}, range=(-infinity,5)",
                    feelsLikeCelsius
            );

            return ThermalSensation.VERY_COLD;
        }

        if (feelsLikeCelsius < 15) {
            log.trace(
                    "Feels-like temperature matched COLD range: "
                            + "feelsLikeCelsius={}, range=[5,15)",
                    feelsLikeCelsius
            );

            return ThermalSensation.COLD;
        }

        if (feelsLikeCelsius < 23) {
            log.trace(
                    "Feels-like temperature matched MILD range: "
                            + "feelsLikeCelsius={}, range=[15,23)",
                    feelsLikeCelsius
            );

            return ThermalSensation.MILD;
        }

        if (feelsLikeCelsius < 28) {
            log.trace(
                    "Feels-like temperature matched WARM range: "
                            + "feelsLikeCelsius={}, range=[23,28)",
                    feelsLikeCelsius
            );

            return ThermalSensation.WARM;
        }

        if (feelsLikeCelsius < 33) {
            log.trace(
                    "Feels-like temperature matched HOT range: "
                            + "feelsLikeCelsius={}, range=[28,33)",
                    feelsLikeCelsius
            );

            return ThermalSensation.HOT;
        }

        log.trace(
                "Feels-like temperature matched VERY_HOT range: "
                        + "feelsLikeCelsius={}, range=[33,+infinity)",
                feelsLikeCelsius
        );

        return ThermalSensation.VERY_HOT;
    }
}