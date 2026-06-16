package com.weatherbridge.application.model;

public record TemperatureAlertSettings(
        boolean enabled,
        double thresholdCelsius
) {

    public TemperatureAlertSettings {
        if (!Double.isFinite(thresholdCelsius)) {
            throw new IllegalArgumentException(
                    "Temperature alert threshold "
                            + "must be finite."
            );
        }
    }
}