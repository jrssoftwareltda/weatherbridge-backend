package com.weatherbridge.application.model;

import java.time.Instant;
import java.util.Objects;

public record WeatherAlertDelivery(
        boolean delivered,
        int statusCode,
        Instant completedAt
) {

    public WeatherAlertDelivery {
        Objects.requireNonNull(
                completedAt,
                "Delivery completion time must not be null."
        );

        if (delivered
                && (statusCode < 200
                || statusCode > 299)) {

            throw new IllegalArgumentException(
                    "A delivered webhook must have "
                            + "a successful HTTP status."
            );
        }

        if (!delivered && statusCode < 0) {
            throw new IllegalArgumentException(
                    "Status code must not be negative."
            );
        }
    }

    public static WeatherAlertDelivery delivered(
            int statusCode,
            Instant completedAt
    ) {
        return new WeatherAlertDelivery(
                true,
                statusCode,
                completedAt
        );
    }

    public static WeatherAlertDelivery skipped(
            Instant completedAt
    ) {
        return new WeatherAlertDelivery(
                false,
                0,
                completedAt
        );
    }
}