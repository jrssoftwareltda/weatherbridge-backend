package com.weatherbridge.domain.service;

import com.weatherbridge.domain.model.ThermalSensation;

public final class ThermalSensationPolicy {

    public ThermalSensation classify(
            double feelsLikeCelsius
    ) {
        if (!Double.isFinite(feelsLikeCelsius)) {
            throw new IllegalArgumentException(
                    "Feels-like temperature must be a finite number."
            );
        }

        if (feelsLikeCelsius < 5) {
            return ThermalSensation.VERY_COLD;
        }

        if (feelsLikeCelsius < 15) {
            return ThermalSensation.COLD;
        }

        if (feelsLikeCelsius < 23) {
            return ThermalSensation.MILD;
        }

        if (feelsLikeCelsius < 28) {
            return ThermalSensation.WARM;
        }

        if (feelsLikeCelsius < 33) {
            return ThermalSensation.HOT;
        }

        return ThermalSensation.VERY_HOT;
    }
}