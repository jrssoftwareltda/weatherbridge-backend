package com.weatherbridge.domain.model;

import java.util.Locale;

public record Location(
        String city,
        String stateCode,
        String countryCode,
        double latitude,
        double longitude,
        int timezoneOffsetSeconds
) {

    public Location {
        city = requireText(
                city,
                "city"
        );

        stateCode = normalizeOptionalCode(
                stateCode
        );

        countryCode = normalizeRequiredCode(
                countryCode,
                "countryCode"
        );

        if (!Double.isFinite(latitude)
                || latitude < -90
                || latitude > 90) {

            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90."
            );
        }

        if (!Double.isFinite(longitude)
                || longitude < -180
                || longitude > 180) {

            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180."
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank."
            );
        }

        return value.trim();
    }

    private static String normalizeOptionalCode(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeRequiredCode(
            String value,
            String fieldName
    ) {
        return requireText(
                value,
                fieldName
        ).toUpperCase(Locale.ROOT);
    }
}