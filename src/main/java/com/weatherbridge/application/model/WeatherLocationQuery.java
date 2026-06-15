package com.weatherbridge.application.model;

import java.text.Normalizer;
import java.util.Locale;

public record WeatherLocationQuery(
        String city,
        String stateCode,
        String countryCode
) {

    private static final String DEFAULT_COUNTRY_CODE =
            "BR";

    public WeatherLocationQuery {
        city = normalizeCity(
                city
        );

        stateCode = normalizeOptionalCode(
                stateCode,
                "stateCode"
        );

        countryCode = normalizeRequiredCode(
                countryCode == null
                        || countryCode.isBlank()
                        ? DEFAULT_COUNTRY_CODE
                        : countryCode,
                "countryCode"
        );
    }

    public String toProviderQuery() {
        if (stateCode == null) {
            return city
                    + ","
                    + countryCode;
        }

        return city
                + ","
                + stateCode
                + ","
                + countryCode;
    }

    public String cacheKey() {
        return normalizeForCache(city)
                + ":"
                + normalizeNullableForCache(stateCode)
                + ":"
                + normalizeForCache(countryCode);
    }

    private static String normalizeCity(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "city must not be blank."
            );
        }

        String normalized = value
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.length() < 2) {
            throw new IllegalArgumentException(
                    "city must have at least 2 characters."
            );
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(
                    "city must have at most 100 characters."
            );
        }

        return normalized;
    }

    private static String normalizeOptionalCode(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return normalizeRequiredCode(
                value,
                fieldName
        );
    }

    private static String normalizeRequiredCode(
            String value,
            String fieldName
    ) {
        String normalized = value
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must contain exactly two letters."
            );
        }

        return normalized;
    }

    private static String normalizeNullableForCache(
            String value
    ) {
        return value == null
                ? "-"
                : normalizeForCache(value);
    }

    private static String normalizeForCache(
            String value
    ) {
        String withoutAccents = Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}