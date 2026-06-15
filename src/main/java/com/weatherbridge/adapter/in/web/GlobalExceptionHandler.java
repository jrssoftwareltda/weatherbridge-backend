package com.weatherbridge.adapter.in.web;

import com.weatherbridge.application.exception.CityNotFoundException;
import com.weatherbridge.application.exception.WeatherProviderException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(
            MissingServletRequestParameterException.class
    )
    public ProblemDetail handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Required query parameter is missing: "
                        + exception.getParameterName(),
                "INVALID_REQUEST",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                "INVALID_REQUEST",
                request
        );
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ProblemDetail handleCityNotFound(
            CityNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "City not found",
                exception.getMessage(),
                "CITY_NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(WeatherProviderException.class)
    public ProblemDetail handleWeatherProvider(
            WeatherProviderException exception,
            HttpServletRequest request
    ) {
        ProviderErrorMapping mapping =
                mapProviderFailure(
                        exception.getFailureType()
                );

        log.warn(
                "Weather provider failure: type={}, message={}",
                exception.getFailureType(),
                exception.getMessage()
        );

        return createProblem(
                mapping.status(),
                mapping.title(),
                exception.getMessage(),
                mapping.code(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing request",
                exception
        );

        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                "INTERNAL_ERROR",
                request
        );
    }

    private ProviderErrorMapping mapProviderFailure(
            WeatherProviderException.FailureType
                    failureType
    ) {
        return switch (failureType) {
            case INVALID_REQUEST ->
                    new ProviderErrorMapping(
                            HttpStatus.BAD_REQUEST,
                            "Invalid weather request",
                            "INVALID_REQUEST"
                    );

            case AUTHENTICATION ->
                    new ProviderErrorMapping(
                            HttpStatus.BAD_GATEWAY,
                            "Weather provider authentication error",
                            "WEATHER_PROVIDER_AUTHENTICATION_ERROR"
                    );

            case RATE_LIMIT ->
                    new ProviderErrorMapping(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Weather provider rate limit exceeded",
                            "WEATHER_PROVIDER_RATE_LIMITED"
                    );

            case TIMEOUT ->
                    new ProviderErrorMapping(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "Weather provider timeout",
                            "WEATHER_PROVIDER_TIMEOUT"
                    );

            case UNAVAILABLE ->
                    new ProviderErrorMapping(
                            HttpStatus.BAD_GATEWAY,
                            "Weather provider unavailable",
                            "WEATHER_PROVIDER_UNAVAILABLE"
                    );

            case INVALID_RESPONSE ->
                    new ProviderErrorMapping(
                            HttpStatus.BAD_GATEWAY,
                            "Invalid weather provider response",
                            "INVALID_PROVIDER_RESPONSE"
                    );
        };
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);

        problem.setType(
                URI.create(
                        "urn:weatherbridge:error:"
                                + code.toLowerCase()
                )
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        problem.setProperty(
                "code",
                code
        );

        problem.setProperty(
                "timestamp",
                Instant.now()
        );

        String correlationId =
                MDC.get("correlationId");

        if (correlationId != null
                && !correlationId.isBlank()) {

            problem.setProperty(
                    "correlationId",
                    correlationId
            );
        }

        return problem;
    }

    private record ProviderErrorMapping(
            HttpStatus status,
            String title,
            String code
    ) {
    }
}