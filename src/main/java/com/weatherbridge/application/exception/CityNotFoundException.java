package com.weatherbridge.application.exception;

public class CityNotFoundException
        extends RuntimeException {

    public CityNotFoundException(
            String city
    ) {
        super(
                "Weather information was not found for city: "
                        + city
        );
    }
}