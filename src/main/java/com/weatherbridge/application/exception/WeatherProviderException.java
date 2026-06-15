package com.weatherbridge.application.exception;

import java.util.Objects;

public class WeatherProviderException
        extends RuntimeException {

    private final FailureType failureType;

    public WeatherProviderException(
            FailureType failureType,
            String message
    ) {
        super(message);

        this.failureType = Objects.requireNonNull(
                failureType,
                "Failure type must not be null."
        );
    }

    public WeatherProviderException(
            FailureType failureType,
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );

        this.failureType = Objects.requireNonNull(
                failureType,
                "Failure type must not be null."
        );
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public enum FailureType {

        INVALID_REQUEST,
        AUTHENTICATION,
        RATE_LIMIT,
        TIMEOUT,
        UNAVAILABLE,
        INVALID_RESPONSE
    }
}