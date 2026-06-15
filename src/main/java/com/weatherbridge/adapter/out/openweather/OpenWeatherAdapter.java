package com.weatherbridge.adapter.out.openweather;

import com.weatherbridge.adapter.out.openweather.dto.OpenWeatherCurrentResponse;
import com.weatherbridge.application.exception.CityNotFoundException;
import com.weatherbridge.application.exception.WeatherProviderException;
import com.weatherbridge.application.model.WeatherLocationQuery;
import com.weatherbridge.application.port.out.WeatherProviderPort;
import com.weatherbridge.domain.model.CurrentWeather;
import com.weatherbridge.infrastructure.properties.OpenWeatherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;

@Component
public class OpenWeatherAdapter
        implements WeatherProviderPort {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OpenWeatherAdapter.class
            );

    private final RestClient restClient;
    private final OpenWeatherProperties properties;
    private final OpenWeatherMapper mapper;

    public OpenWeatherAdapter(
            @Qualifier("openWeatherRestClient")
            RestClient restClient,
            OpenWeatherProperties properties,
            OpenWeatherMapper mapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    @Cacheable(
            cacheNames = "current-weather",
            key = "#query.cacheKey()",
            sync = true
    )
    public CurrentWeather getCurrentWeather(
            WeatherLocationQuery query
    ) {
        log.debug(
                "Requesting current weather: city={}, stateCode={}, countryCode={}",
                query.city(),
                query.stateCode(),
                query.countryCode()
        );

        try {
            OpenWeatherCurrentResponse response =
                    restClient
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/weather")
                                    .queryParam(
                                            "q",
                                            query.toProviderQuery()
                                    )
                                    .queryParam(
                                            "appid",
                                            properties.apiKey()
                                    )
                                    .queryParam(
                                            "units",
                                            properties.units()
                                    )
                                    .queryParam(
                                            "lang",
                                            properties.language()
                                    )
                                    .build()
                            )
                            .retrieve()
                            .body(
                                    OpenWeatherCurrentResponse.class
                            );

            return mapper.toDomain(
                    response,
                    query
            );

        } catch (RestClientResponseException exception) {
            throw mapResponseException(
                    exception,
                    query
            );

        } catch (ResourceAccessException exception) {
            throw mapResourceAccessException(
                    exception
            );

        } catch (RestClientException exception) {
            throw new WeatherProviderException(
                    WeatherProviderException.FailureType
                            .UNAVAILABLE,
                    "Unable to communicate with OpenWeather.",
                    exception
            );
        }
    }

    private RuntimeException mapResponseException(
            RestClientResponseException exception,
            WeatherLocationQuery query
    ) {
        int statusCode =
                exception.getStatusCode().value();

        return switch (statusCode) {
            case 400 ->
                    new WeatherProviderException(
                            WeatherProviderException.FailureType
                                    .INVALID_REQUEST,
                            "OpenWeather rejected the location query."
                    );

            case 401 ->
                    new WeatherProviderException(
                            WeatherProviderException.FailureType
                                    .AUTHENTICATION,
                            "OpenWeather rejected the configured API key."
                    );

            case 404 ->
                    new CityNotFoundException(
                            query.city()
                    );

            case 429 ->
                    new WeatherProviderException(
                            WeatherProviderException.FailureType
                                    .RATE_LIMIT,
                            "OpenWeather rate limit was reached."
                    );

            default -> {
                if (statusCode >= 500) {
                    yield new WeatherProviderException(
                            WeatherProviderException.FailureType
                                    .UNAVAILABLE,
                            "OpenWeather is temporarily unavailable."
                    );
                }

                yield new WeatherProviderException(
                        WeatherProviderException.FailureType
                                .UNAVAILABLE,
                        "OpenWeather returned an unexpected response."
                );
            }
        };
    }

    private WeatherProviderException
    mapResourceAccessException(
            ResourceAccessException exception
    ) {
        if (hasTimeoutCause(exception)) {
            return new WeatherProviderException(
                    WeatherProviderException.FailureType
                            .TIMEOUT,
                    "OpenWeather did not respond within the configured timeout.",
                    exception
            );
        }

        return new WeatherProviderException(
                WeatherProviderException.FailureType
                        .UNAVAILABLE,
                "OpenWeather could not be reached.",
                exception
        );
    }

    private boolean hasTimeoutCause(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}