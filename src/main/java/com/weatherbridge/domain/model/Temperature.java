package com.weatherbridge.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Temperature(
        double value,
        double feelsLike,
        double difference,
        TemperatureUnit unit,
        ThermalSensation category
) {

    public Temperature {
        validateFinite(value, "value");
        validateFinite(feelsLike, "feelsLike");
        validateFinite(difference, "difference");

        unit = Objects.requireNonNull(
                unit,
                "Temperature unit must not be null."
        );

        category = Objects.requireNonNull(
                category,
                "Thermal sensation category must not be null."
        );
    }

    public static Temperature celsius(
            double value,
            double feelsLike,
            ThermalSensation category
    ) {
        return new Temperature(
                roundOneDecimal(value),
                roundOneDecimal(feelsLike),
                roundOneDecimal(feelsLike - value),
                TemperatureUnit.CELSIUS,
                category
        );
    }

    private static double roundOneDecimal(
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

    private static void validateFinite(
            double value,
            String fieldName
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    fieldName + " must be a finite number."
            );
        }
    }
}