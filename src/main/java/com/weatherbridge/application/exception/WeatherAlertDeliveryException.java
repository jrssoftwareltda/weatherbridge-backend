package com.weatherbridge.application.exception;

public final class WeatherAlertDeliveryException
        extends RuntimeException {

    public WeatherAlertDeliveryException(
            String message
    ) {
        super(message);
    }

    public WeatherAlertDeliveryException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}